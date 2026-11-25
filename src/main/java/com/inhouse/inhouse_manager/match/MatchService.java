package com.inhouse.inhouse_manager.match;

import com.inhouse.inhouse_manager.common.NotFoundException;
import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerRepository;
import com.inhouse.inhouse_manager.player.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {
    private static final int TEAM_SIZE = Position.values().length;

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    /**
     * bluePlayerIds, redPlayerIds는 포지션 순서(TOP, JUNGLE, MID, ADC, SUPPORT)대로 들어 있다.
     */
    @Transactional
    public Long record(LocalDate playedAt, Team winner, List<Long> bluePlayerIds, List<Long> redPlayerIds) {
        if (playedAt.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("미래 날짜는 기록할 수 없습니다.");
        }
        if (!isFilled(bluePlayerIds) || !isFilled(redPlayerIds)) {
            throw new IllegalArgumentException("모든 자리에 플레이어를 선택하세요.");
        }

        List<Long> allIds = Stream.concat(bluePlayerIds.stream(), redPlayerIds.stream()).toList();
        Map<Long, Player> players = playerRepository.findAllById(allIds).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        if (!players.keySet().containsAll(allIds)) {
            throw new IllegalArgumentException("존재하지 않는 플레이어가 있습니다.");
        }

        Set<Long> seen = new HashSet<>();
        for (Long id : allIds) {
            if (!seen.add(id)) {
                throw new IllegalArgumentException("같은 플레이어를 두 자리에 넣을 수 없습니다: " + players.get(id).getName());
            }
        }

        Match match = new Match(playedAt, winner);
        Position[] positions = Position.values();
        for (int i = 0; i < TEAM_SIZE; i++) {
            match.addParticipant(players.get(bluePlayerIds.get(i)), Team.BLUE, positions[i]);
            match.addParticipant(players.get(redPlayerIds.get(i)), Team.RED, positions[i]);
        }
        return matchRepository.save(match).getId();
    }

    public List<Match> findAll() {
        return matchRepository.findAllWithParticipants();
    }

    public Match findById(Long id) {
        return matchRepository.findWithParticipantsById(id)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 경기입니다: " + id));
    }

    @Transactional
    public void delete(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 경기입니다: " + id));
        matchRepository.delete(match);
    }

    private boolean isFilled(List<Long> playerIds) {
        return playerIds != null && playerIds.size() == TEAM_SIZE && playerIds.stream().noneMatch(Objects::isNull);
    }
}
