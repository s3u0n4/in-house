package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerRepository;
import com.inhouse.inhouse_manager.player.Tier;
import com.inhouse.inhouse_manager.stats.PlayerStats;
import com.inhouse.inhouse_manager.stats.StatsService;
import com.inhouse.inhouse_manager.team.TeamBalancer.Candidate;
import com.inhouse.inhouse_manager.team.TeamBalancer.TeamSplit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {
    /** 아무도 티어가 없을 때 쓰는 점수 (골드). 모두 같은 점수라 결과에는 영향이 없다 */
    private static final double DEFAULT_TIER_SCORE = Tier.GOLD.getScore();

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
                .sorted(Comparator.comparing(Player::getId))
                .toList();
        if (players.size() != TeamBalancer.PLAYER_COUNT) {
            throw new IllegalArgumentException("존재하지 않는 플레이어가 있습니다.");
        }

        // 언랭이나 티어 미입력은 오늘 참가자 중 티어가 있는 사람들의 평균 점수로 본다
        double averageTier = players.stream()
                .map(TeamService::tierScoreOf)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(DEFAULT_TIER_SCORE);

        Map<Long, PlayerStats> stats = statsService.statsByPlayer(distinctIds);
        List<Candidate> candidates = players.stream()
                .map(player -> {
                    Integer score = tierScoreOf(player);
                    return new Candidate(player.getId(), player.getName(), player.getMainPosition(),
                            player.getSubPosition(), tierLabelOf(player),
                            score != null ? score : averageTier, stats.get(player.getId()).rating());
                })
                .toList();
        return teamBalancer.split(candidates);
    }

    private static Integer tierScoreOf(Player player) {
        return player.getTier() == null ? null : player.getTier().getScore();
    }

    private static String tierLabelOf(Player player) {
        if (player.getTier() == null) {
            return "미입력";
        }
        return player.getTier().getLabel();
    }
}
