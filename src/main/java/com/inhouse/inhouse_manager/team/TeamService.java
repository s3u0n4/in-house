package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerRepository;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.player.Tier;
import com.inhouse.inhouse_manager.stats.PlayerStats;
import com.inhouse.inhouse_manager.stats.StatsService;
import com.inhouse.inhouse_manager.team.TeamBalancer.Candidate;
import com.inhouse.inhouse_manager.team.TeamBalancer.TeamSplit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {
    /** 아무도 라인별 티어가 없을 때 쓰는 점수 (골드). 모두 같은 점수라 결과에는 영향이 없다 */
    private static final double DEFAULT_TIER_SCORE = Tier.GOLD.getScore();
    /** 랜덤 교체 때 허용하는 불균형 증가량 (대략 티어 2단계) */
    static final double SHUFFLE_TOLERANCE = 2.0;
    /** 허용 범위 안의 조합이 이보다 적으면 불균형이 작은 순으로 이만큼은 후보로 쓴다 */
    private static final int MIN_SHUFFLE_OPTIONS = 3;

    private final PlayerRepository playerRepository;
    private final StatsService statsService;
    private final TeamBalancer teamBalancer;

    public TeamSplit split(List<Long> playerIds) {
        List<Long> distinctIds = playerIds == null ? List.of()
                : playerIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.size() != TeamBalancer.PLAYER_COUNT) {
            throw new IllegalArgumentException("참가자를 정확히 " + TeamBalancer.PLAYER_COUNT + "명 골라 주세요. (현재 "
                    + distinctIds.size() + "명)");
        }

        // id 순으로 정렬해 두면 같은 참가자를 고르면 항상 같은 결과가 나온다
        List<Player> players = playerRepository.findAllById(distinctIds).stream()
                .filter(player -> !player.isDeleted())
                .sorted(Comparator.comparing(Player::getId))
                .toList();
        if (players.size() != TeamBalancer.PLAYER_COUNT) {
            throw new IllegalArgumentException("존재하지 않는 플레이어가 있습니다.");
        }

        // 라인별 티어가 없는 사람(기능 전에 등록)은 오늘 참가자들의 주 라인 티어 평균으로 본다
        double averageTier = players.stream()
                .filter(Player::hasLaneTiers)
                .mapToDouble(player -> player.getLaneTiers().scoreOf(player.getMainPosition()))
                .average()
                .orElse(DEFAULT_TIER_SCORE);

        Map<Long, PlayerStats> stats = statsService.statsByPlayer(distinctIds);
        List<Candidate> candidates = players.stream()
                .map(player -> new Candidate(player.getId(), player.getName(), player.getMainPosition(),
                        player.getSubPosition(), "미입력", averageTier, stats.get(player.getId()).rating(),
                        player.hasLaneTiers() ? player.getLaneTiers() : null))
                .toList();
        return teamBalancer.split(candidates);
    }

    /**
     * 랜덤 교체 후보: 같은 라인끼리 짝지은 두 사람을 맞바꾸는 조합 중 균형이 크게 깨지지 않는 것들.
     * 5개 라인 모두 바꿀지 말지를 정하는 조합 중, 하나도 안 바꾸는 것과 다섯 쌍을 다 바꾸는 것(블루/레드 이름만 뒤집힌
     * 같은 팀)을 뺀 30가지를 본다.
     * 불균형 점수가 원래 + SHUFFLE_TOLERANCE 이내인 조합만 남기고, 그런 조합이 너무 적으면 불균형이 작은 순으로 몇 개를 쓴다.
     */
    public List<Set<Position>> fairSwapOptions(TeamSplit split) {
        Position[] positions = Position.values();
        List<Set<Position>> all = new ArrayList<>();
        for (int mask = 1; mask < (1 << positions.length) - 1; mask++) {
            Set<Position> swap = EnumSet.noneOf(Position.class);
            for (int i = 0; i < positions.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    swap.add(positions[i]);
                }
            }
            all.add(swap);
        }
        Comparator<Set<Position>> byCost = Comparator.comparingDouble(swap -> teamBalancer.swapPairs(split, swap).cost());
        List<Set<Position>> fair = all.stream()
                .filter(swap -> teamBalancer.swapPairs(split, swap).cost() <= split.cost() + SHUFFLE_TOLERANCE + 1e-9)
                .toList();
        if (fair.size() >= MIN_SHUFFLE_OPTIONS) {
            return fair;
        }
        return all.stream().sorted(byCost).limit(MIN_SHUFFLE_OPTIONS).toList();
    }

    /** 후보 중 하나를 고른다. seed가 같으면 항상 같은 쌍을 고른다 (주소에 seed를 넣어 두면 새로고침해도 결과가 같다) */
    public Set<Position> pairsToSwap(TeamSplit split, long seed) {
        List<Set<Position>> options = fairSwapOptions(split);
        // java.util.Random은 seed가 비슷하면 첫 값도 비슷하게 나와서, seed를 잘 섞어 주는 SplittableRandom을 쓴다
        return options.get(new SplittableRandom(seed).nextInt(options.size()));
    }

    public TeamSplit swapPairs(TeamSplit split, Set<Position> positions) {
        return teamBalancer.swapPairs(split, positions);
    }
}
