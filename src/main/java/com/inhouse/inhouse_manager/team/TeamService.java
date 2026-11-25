package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerRepository;
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

        Map<Long, PlayerStats> stats = statsService.statsByPlayer(distinctIds);
        List<Candidate> candidates = players.stream()
                .map(player -> new Candidate(player.getId(), player.getName(), player.getMainPosition(),
                        player.getSubPosition(), stats.get(player.getId()).rating()))
                .toList();
        return teamBalancer.split(candidates);
    }
}
