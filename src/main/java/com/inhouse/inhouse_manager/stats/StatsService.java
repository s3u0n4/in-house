package com.inhouse.inhouse_manager.stats;

import com.inhouse.inhouse_manager.match.MatchParticipant;
import com.inhouse.inhouse_manager.match.MatchParticipantRepository;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.stats.PlayerRecord.PositionStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {
    private static final int RECENT_MATCH_COUNT = 10;

    private final MatchParticipantRepository matchParticipantRepository;

    /**
     * 플레이어 id별 전적. 경기 기록이 없는 플레이어도 빈 전적(0경기)으로 들어 있다.
     */
    public Map<Long, PlayerStats> statsByPlayer(Collection<Long> playerIds) {
        Map<Long, PlayerStats> result = new HashMap<>();
        for (Long playerId : playerIds) {
            result.put(playerId, PlayerStats.EMPTY);
        }
        for (MatchParticipant participant : matchParticipantRepository.findAllWithMatch()) {
            Long playerId = participant.getPlayer().getId();
            if (result.containsKey(playerId)) {
                result.put(playerId, result.get(playerId).plus(participant.isWin()));
            }
        }
        return result;
    }

    public PlayerRecord recordOf(Long playerId) {
        List<MatchParticipant> participants = matchParticipantRepository.findAllByPlayerIdWithMatch(playerId);

        PlayerStats total = PlayerStats.EMPTY;
        Map<Position, PlayerStats> byPosition = new EnumMap<>(Position.class);
        for (MatchParticipant participant : participants) {
            total = total.plus(participant.isWin());
            PlayerStats before = byPosition.getOrDefault(participant.getPosition(), PlayerStats.EMPTY);
            byPosition.put(participant.getPosition(), before.plus(participant.isWin()));
        }

        List<PositionStats> positionStats = byPosition.entrySet().stream()
                .map(entry -> new PositionStats(entry.getKey(), entry.getValue()))
                .toList();
        List<MatchParticipant> recent = participants.stream().limit(RECENT_MATCH_COUNT).toList();
        return new PlayerRecord(total, positionStats, recent);
    }
}
