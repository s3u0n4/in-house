package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 10명을 실력이 비슷한 두 팀으로 나누고, 팀마다 포지션을 배치한다.
 * 규칙은 docs/requirements.md 3.4 참고.
 */
@Component
public class TeamBalancer {
    public static final int TEAM_SIZE = Position.values().length;
    public static final int PLAYER_COUNT = TEAM_SIZE * 2;

    /** 점수 차이가 최소 차이 + 이 값 이내면 "비슷하다"고 보고 포지션 적합도로 고른다. */
    static final double SCORE_TOLERANCE = 0.1;

    private static final List<int[]> POSITION_ORDERS = permutations(TEAM_SIZE);

    public record Candidate(Long playerId, String name, Position mainPosition, Position subPosition, double rating) {
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
        public double score() {
            return assignments.stream().mapToDouble(a -> a.player().rating()).sum();
        }

        public int fit() {
            return assignments.stream().mapToInt(Assignment::fit).sum();
        }

        public List<Long> playerIds() {
            return assignments.stream().map(a -> a.player().playerId()).toList();
        }
    }

    public record TeamSplit(Lineup blue, Lineup red) {
        public double scoreDiff() {
            return Math.abs(blue.score() - red.score());
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

        double minDiff = splits.stream().mapToDouble(TeamSplit::scoreDiff).min().orElseThrow();
        return splits.stream()
                .filter(split -> split.scoreDiff() <= minDiff + SCORE_TOLERANCE + 1e-9)
                .max(Comparator.comparingInt(TeamSplit::fit)
                        .thenComparing(Comparator.comparingDouble(TeamSplit::scoreDiff).reversed()))
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
