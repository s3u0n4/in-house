package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.LaneTiers;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.player.Tier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 10명을 실력이 비슷한 두 팀으로 나누고, 팀마다 포지션을 배치한다.
 * 규칙은 docs/requirements.md 3.3, 3.4 참고.
 */
@Component
public class TeamBalancer {
    public static final int TEAM_SIZE = Position.values().length;
    public static final int PLAYER_COUNT = TEAM_SIZE * 2;

    /** 내전 승률 보정의 크기. 보정 승률이 0.5에서 멀어질수록 최대 ±2 티어만큼 전력 점수가 바뀐다 */
    static final double WIN_RATE_WEIGHT = 4.0;
    /** 라인 맞대결 차이를 전력 차이와 비교할 때 곱하는 값 */
    static final double LANE_WEIGHT = 0.5;
    /** 불균형 점수가 최소값 + 이 값 이내면 "비슷하다"고 보고 포지션 적합도로 고른다 */
    static final double COST_TOLERANCE = 0.5;
    /**
     * 라인별 티어가 없는 사람이 주 포지션이 아닐 때 빼는 전력 점수. 부 포지션이면 1번, 그 외 포지션이면 2번 뺀다 (티어 1~2단계).
     * 라인별 티어를 넣은 사람은 그 라인의 티어를 그대로 쓰고 감점하지 않는다.
     */
    static final double OFF_ROLE_PENALTY = 1.0;
    /** 라인 맞대결 차이가 모든 라인에서 이 값(티어 1단계) 이하여야 "라인별 밸런스가 맞다"고 보고 랜덤 교체를 허용한다 */
    static final double LANE_BALANCE_LIMIT = 1.0;

    private static final List<int[]> POSITION_ORDERS = permutations(TEAM_SIZE);

    /** 보정 승률(0~1)을 전력 점수 보정값(-2 ~ +2)으로 바꾼다. 랭킹 화면의 전력도 이 값을 쓴다 */
    public static double ratingBonus(double rating) {
        return (rating - 0.5) * WIN_RATE_WEIGHT;
    }

    /**
     * laneTiers: 라인별 티어. 다 넣은 사람만 있고, 아니면 null
     * tierScore, tierLabel: 라인별 티어가 없을 때 쓰는 티어 점수와 이름 (TeamService가 참가자 평균으로 채워서 넘긴다).
     *   이때는 주/부 포지션으로 감점한다
     * rating: 내전 보정 승률 (승+1)/(경기+2)
     */
    public record Candidate(Long playerId, String name, Position mainPosition, Position subPosition,
                            String tierLabel, double tierScore, double rating, LaneTiers laneTiers) {
        /** 라인별 티어가 없는 후보 */
        public Candidate(Long playerId, String name, Position mainPosition, Position subPosition,
                         String tierLabel, double tierScore, double rating) {
            this(playerId, name, mainPosition, subPosition, tierLabel, tierScore, rating, null);
        }

        /** 내전 승률 보정. 보정 승률이 0.5에서 멀어질수록 최대 ±2 */
        public double ratingBonus() {
            return TeamBalancer.ratingBonus(rating);
        }
    }

    public record Assignment(Candidate player, Position position) {
        public int fit() {
            if (position == player.mainPosition()) return 2;
            if (position == player.subPosition()) return 1;
            return 0;
        }

        /** 이 포지션에서의 티어 점수. 라인별 티어가 있으면 그 라인의 티어(마스터 이상은 LP 반영), 없으면 tierScore */
        public double tierScore() {
            return player.laneTiers() != null ? player.laneTiers().scoreOf(position) : player.tierScore();
        }

        /** 화면에 보여줄 티어 이름. 예: "다이아몬드", "마스터 350LP" */
        public String tierLabel() {
            return player.laneTiers() != null ? player.laneTiers().labelOf(position) : player.tierLabel();
        }

        /** 포지션 감점. 라인별 티어가 없는 사람만: 주 포지션 0, 부 포지션 -1, 그 외 -2 */
        public double positionPenalty() {
            return player.laneTiers() != null ? 0 : (2 - fit()) * OFF_ROLE_PENALTY;
        }

        /** 이 포지션을 맡았을 때의 전력 = 이 포지션의 티어 점수 + 내전 승률 보정 - 포지션 감점 */
        public double power() {
            return tierScore() + player.ratingBonus() - positionPenalty();
        }

        /**
         * 라인 맞대결에서 쓰는 전력 (2026-11-28). 라인별 티어가 있어도 주 포지션이 아니면 낮게 본다: 부 포지션 -1, 그 외 -2.
         * 그래서 부 라인으로 간 사람은 자기 라인 티어보다 낮은 주 라인 상대와 맞붙게 된다. 팀 전력(power)에는 넣지 않는다.
         * 라인별 티어가 없는 사람은 power()에 이미 같은 감점이 들어 있어서 power()와 같다.
         */
        public double matchupPower() {
            return tierScore() + player.ratingBonus() - (2 - fit()) * OFF_ROLE_PENALTY;
        }

        public String fitLabel() {
            return switch (fit()) {
                case 2 -> "주";
                case 1 -> "부";
                default -> "-";
            };
        }
    }

    /** 포지션 순서(TOP~SUPPORT)로 정렬된 팀 배치 */
    public record Lineup(List<Assignment> assignments) {
        /** 팀 전력 = 팀원이 맡은 포지션 기준 전력의 합 (주 포지션이 아니면 감점된 값) */
        public double power() {
            return assignments.stream().mapToDouble(Assignment::power).sum();
        }

        public double positionPenalty() {
            return assignments.stream().mapToDouble(Assignment::positionPenalty).sum();
        }

        public double averageTier() {
            return assignments.stream().mapToDouble(Assignment::tierScore).average().orElse(0);
        }

        /** 평균 티어 점수에 가장 가까운 티어 이름 (예: 골드) */
        public String averageTierLabel() {
            return Tier.nearestLabel(averageTier());
        }

        public int fit() {
            return assignments.stream().mapToInt(Assignment::fit).sum();
        }

        public List<Long> playerIds() {
            return assignments.stream().map(a -> a.player().playerId()).toList();
        }

    }

    public record TeamSplit(Lineup blue, Lineup red) {
        /** 두 팀 전력 점수 합의 차이 */
        public double powerDiff() {
            return Math.abs(blue.power() - red.power());
        }

        /**
         * 라인 맞대결 차이: 블루 탑 ↔ 레드 탑, 블루 정글 ↔ 레드 정글 … 같은 라인끼리 전력 차이를 더한 값 (2026-11-26).
         * 두 팀 합이 같아도 "탑은 다이아 대 아이언, 미드는 아이언 대 다이아"처럼 라인마다 한쪽이 압도하면 커진다.
         * 라인끼리 비슷하면 그 두 사람을 맞바꿔도(랜덤 교체) 균형이 거의 그대로다.
         * 주 라인 대 부 라인처럼 맞붙으면 부 라인 쪽을 낮게 본다 (Assignment.matchupPower).
         */
        public double laneDiff() {
            double diff = 0;
            for (int i = 0; i < TEAM_SIZE; i++) {
                diff += Math.abs(blue.assignments().get(i).matchupPower() - red.assignments().get(i).matchupPower());
            }
            return diff;
        }

        /**
         * 맞대결 차이가 LANE_BALANCE_LIMIT보다 큰 라인 (2026-11-28). 비어 있으면 라인마다 상대가 비슷해서 랜덤 교체를 할 수 있다.
         * 비어 있지 않으면 라인끼리는 안 맞고 다른 라인으로 팀 전력을 맞춘 것이라, 그 라인을 맞바꾸면 균형이 깨진다.
         */
        public List<Position> unbalancedLanes() {
            List<Position> lanes = new ArrayList<>();
            for (int i = 0; i < TEAM_SIZE; i++) {
                Assignment blueSlot = blue.assignments().get(i);
                if (Math.abs(blueSlot.matchupPower() - red.assignments().get(i).matchupPower()) > LANE_BALANCE_LIMIT + 1e-9) {
                    lanes.add(blueSlot.position());
                }
            }
            return lanes;
        }

        /** 불균형 점수. 작을수록 공평하다 */
        public double cost() {
            return powerDiff() + LANE_WEIGHT * laneDiff();
        }

        int fit() {
            return blue.fit() + red.fit();
        }

        /** 두 팀 전력의 합. 균형이 비슷하면 이 값이 큰 쪽(다들 잘하는 라인을 맡는 쪽)을 고른다. 소수점 오차를 없애려고 반올림한다 */
        long totalPower() {
            return Math.round((blue.power() + red.power()) * 1000);
        }
    }

    public TeamSplit split(List<Candidate> candidates) {
        if (candidates.size() != PLAYER_COUNT) {
            throw new IllegalArgumentException("참가자는 " + PLAYER_COUNT + "명이어야 합니다.");
        }

        // 0번 참가자는 항상 블루팀에 넣는다. 그래야 (A팀, B팀)과 (B팀, A팀)을 두 번 세지 않는다. → 126가지
        List<TeamSplit> splits = new ArrayList<>();
        for (int mask = 0; mask < (1 << PLAYER_COUNT); mask++) {
            if ((mask & 1) == 0 || Integer.bitCount(mask) != TEAM_SIZE) {
                continue;
            }
            List<Candidate> blue = new ArrayList<>();
            List<Candidate> red = new ArrayList<>();
            for (int i = 0; i < PLAYER_COUNT; i++) {
                ((mask & (1 << i)) != 0 ? blue : red).add(candidates.get(i));
            }
            splits.add(bestLaneMatch(bestLineups(blue), bestLineups(red)));
        }

        double minCost = splits.stream().mapToDouble(TeamSplit::cost).min().orElseThrow();
        return splits.stream()
                .filter(split -> split.cost() <= minCost + COST_TOLERANCE + 1e-9)
                .max(Comparator.comparingLong(TeamSplit::totalPower)
                        .thenComparingInt(TeamSplit::fit)
                        .thenComparing(Comparator.comparingDouble(TeamSplit::cost).reversed()))
                .orElseThrow();
    }

    /**
     * 같은 포지션끼리 짝지은 두 사람(블루 TOP ↔ 레드 TOP ...) 중 positions에 든 쌍을 서로 맞바꾼다.
     * 포지션은 그대로라서 포지션 적합도는 변하지 않는다.
     */
    public TeamSplit swapPairs(TeamSplit split, Set<Position> positions) {
        List<Assignment> blue = new ArrayList<>();
        List<Assignment> red = new ArrayList<>();
        // 두 팀 모두 포지션 순서로 정렬되어 있어서 같은 번째끼리 같은 포지션이다
        for (int i = 0; i < TEAM_SIZE; i++) {
            Assignment blueSlot = split.blue().assignments().get(i);
            Assignment redSlot = split.red().assignments().get(i);
            if (positions.contains(blueSlot.position())) {
                blue.add(new Assignment(redSlot.player(), blueSlot.position()));
                red.add(new Assignment(blueSlot.player(), redSlot.position()));
            } else {
                blue.add(blueSlot);
                red.add(redSlot);
            }
        }
        return new TeamSplit(new Lineup(blue), new Lineup(red));
    }

    /**
     * 5명을 5개 포지션에 배치하는 120가지 중 팀 전력이 가장 높은 배치들 (전력이 같으면 주 포지션이 많은 배치만).
     * 라인별 티어가 있는 사람은 잘하는 라인으로, 없는 사람은 주 포지션 → 부 포지션 순으로 간다.
     * 똑같이 좋은 배치가 여러 개일 수 있어서 모두 돌려준다 (그중 상대 팀과 라인 맞대결이 가장 비슷한 것을 고른다).
     */
    private List<Lineup> bestLineups(List<Candidate> members) {
        Position[] positions = Position.values();
        List<Lineup> best = new ArrayList<>();
        for (int[] order : POSITION_ORDERS) {
            List<Assignment> assignments = new ArrayList<>();
            for (int i = 0; i < TEAM_SIZE; i++) {
                assignments.add(new Assignment(members.get(i), positions[order[i]]));
            }
            assignments.sort(Comparator.comparing(Assignment::position));
            Lineup lineup = new Lineup(assignments);
            int compare = best.isEmpty() ? 1 : compareLineups(lineup, best.get(0));
            if (compare > 0) {
                best.clear();
            }
            if (compare >= 0) {
                best.add(lineup);
            }
        }
        return best;
    }

    /** 전력이 높으면 더 좋은 배치, 전력이 같으면 주 포지션이 많은 배치 */
    private static int compareLineups(Lineup a, Lineup b) {
        if (Math.abs(a.power() - b.power()) > 1e-9) {
            return a.power() > b.power() ? 1 : -1;
        }
        return Integer.compare(a.fit(), b.fit());
    }

    /** 두 팀의 가장 좋은 배치끼리 짝지어 보고, 라인 맞대결 차이가 가장 작은 조합 */
    private static TeamSplit bestLaneMatch(List<Lineup> blues, List<Lineup> reds) {
        TeamSplit best = null;
        for (Lineup blue : blues) {
            for (Lineup red : reds) {
                TeamSplit split = new TeamSplit(blue, red);
                if (best == null || split.laneDiff() < best.laneDiff() - 1e-9) {
                    best = split;
                }
            }
        }
        return best;
    }

    private static List<int[]> permutations(int n) {
        List<int[]> result = new ArrayList<>();
        permute(new int[n], new boolean[n], 0, result);
        return result;
    }

    private static void permute(int[] current, boolean[] used, int depth, List<int[]> result) {
        if (depth == current.length) {
            result.add(current.clone());
            return;
        }
        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                permute(current, used, depth + 1, result);
                used[i] = false;
            }
        }
    }
}
