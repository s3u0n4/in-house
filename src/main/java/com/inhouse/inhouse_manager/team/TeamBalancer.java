package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.player.Tier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    /** 티어 분포 차이를 전력 차이와 비교할 때 곱하는 값 */
    static final double DISTRIBUTION_WEIGHT = 0.5;
    /** 불균형 점수가 최소값 + 이 값 이내면 "비슷하다"고 보고 포지션 적합도로 고른다 */
    static final double COST_TOLERANCE = 0.5;

    private static final List<int[]> POSITION_ORDERS = permutations(TEAM_SIZE);

    /**
     * tierScore: 티어 점수 (언랭이나 미입력은 TeamService가 참가자 평균으로 채워서 넘긴다)
     * rating: 내전 보정 승률 (승+1)/(경기+2)
     */
    public record Candidate(Long playerId, String name, Position mainPosition, Position subPosition,
                            String tierLabel, double tierScore, double rating) {
        /** 전력 점수 = 티어 점수 + 내전 승률 보정 */
        public double power() {
            return tierScore + (rating - 0.5) * WIN_RATE_WEIGHT;
        }
    }

    public record Assignment(Candidate player, Position position) {
        public int fit() {
            if (position == player.mainPosition()) return 2;
            if (position == player.subPosition()) return 1;
            return 0;
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
        public double power() {
            return assignments.stream().mapToDouble(a -> a.player().power()).sum();
        }

        public double averageTier() {
            return assignments.stream().mapToDouble(a -> a.player().tierScore()).average().orElse(0);
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

        /** 티어 점수를 높은 순으로 정렬한 목록 (분포 비교용) */
        List<Double> tierScoresDescending() {
            return assignments.stream().map(a -> a.player().tierScore()).sorted(Comparator.reverseOrder()).toList();
        }
    }

    public record TeamSplit(Lineup blue, Lineup red) {
        /** 두 팀 전력 점수 합의 차이 */
        public double powerDiff() {
            return Math.abs(blue.power() - red.power());
        }

        /**
         * 티어 분포 차이: 각 팀 티어를 높은 순으로 세워 1등끼리, 2등끼리 ... 비교한 차이의 합.
         * 합이 같아도 "다이아 둘 + 아이언 둘" 대 "골드 넷"처럼 구성이 다르면 커진다.
         */
        public double distributionDiff() {
            List<Double> blueTiers = blue.tierScoresDescending();
            List<Double> redTiers = red.tierScoresDescending();
            double diff = 0;
            for (int i = 0; i < blueTiers.size(); i++) {
                diff += Math.abs(blueTiers.get(i) - redTiers.get(i));
            }
            return diff;
        }

        /** 불균형 점수. 작을수록 공평하다 */
        public double cost() {
            return powerDiff() + DISTRIBUTION_WEIGHT * distributionDiff();
        }

        int fit() {
            return blue.fit() + red.fit();
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
            splits.add(new TeamSplit(bestLineup(blue), bestLineup(red)));
        }

        double minCost = splits.stream().mapToDouble(TeamSplit::cost).min().orElseThrow();
        return splits.stream()
                .filter(split -> split.cost() <= minCost + COST_TOLERANCE + 1e-9)
                .max(Comparator.comparingInt(TeamSplit::fit)
                        .thenComparing(Comparator.comparingDouble(TeamSplit::cost).reversed()))
                .orElseThrow();
    }

    /** 5명을 5개 포지션에 배치하는 120가지 중 적합도가 가장 높은 배치 */
    private Lineup bestLineup(List<Candidate> members) {
        Position[] positions = Position.values();
        Lineup best = null;
        for (int[] order : POSITION_ORDERS) {
            List<Assignment> assignments = new ArrayList<>();
            for (int i = 0; i < TEAM_SIZE; i++) {
                assignments.add(new Assignment(members.get(i), positions[order[i]]));
            }
            assignments.sort(Comparator.comparing(Assignment::position));
            Lineup lineup = new Lineup(assignments);
            if (best == null || lineup.fit() > best.fit()) {
                best = lineup;
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
