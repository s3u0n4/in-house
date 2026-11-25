package com.inhouse.inhouse_manager.player;

import com.inhouse.inhouse_manager.common.NotFoundException;
import com.inhouse.inhouse_manager.match.MatchParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    @Transactional
    public Long register(String name, String riotId, Position mainPosition, Position subPosition, Tier tier) {
        String trimmedRiotId = riotId.strip();
        if (playerRepository.existsByRiotId(trimmedRiotId)){
            throw new IllegalArgumentException("이미 등록된 라이엇 ID입니다: " + trimmedRiotId);
        }
        Player player = new Player(name.strip(), trimmedRiotId, mainPosition, subPosition, tier);
        return playerRepository.save(player).getId();
    }

    @Transactional
    public void update(Long id, String name, String riotId, Position mainPosition, Position subPosition, Tier tier) {
        Player player = findById(id);
        String trimmedRiotId = riotId.strip();
        if (playerRepository.existsByRiotIdAndIdNot(trimmedRiotId, id)) {
            throw new IllegalArgumentException("이미 등록된 라이엇 ID입니다: " + trimmedRiotId);
        }
        // 트랜잭션이 끝날 때 JPA가 바뀐 필드를 찾아 UPDATE 한다 (변경 감지). save()를 다시 부를 필요가 없다.
        player.update(name.strip(), trimmedRiotId, mainPosition, subPosition, tier);
    }

    /**
     * 여러 명을 한 번에 수정한다. 하나의 트랜잭션이라 중간에 하나라도 실패하면 전부 저장되지 않는다.
     */
    @Transactional
    public void updateAll(List<PlayerUpdate> updates) {
        saveAll(updates, List.of());
    }

    /**
     * 목록 수정 저장: deleteIds를 삭제하고 updates를 수정한다. 하나의 트랜잭션이라 하나라도 실패하면 아무것도 바뀌지 않는다.
     */
    @Transactional
    public void saveAll(List<PlayerUpdate> updates, List<Long> deleteIds) {
        List<String> blocked = deleteIds.stream()
                .filter(matchParticipantRepository::existsByPlayerId)
                .map(id -> findById(id).getName())
                .toList();
        if (!blocked.isEmpty()) {
            throw new IllegalStateException("경기 기록이 있는 플레이어는 삭제할 수 없습니다: " + String.join(", ", blocked));
        }

        Set<String> riotIds = new HashSet<>();
        for (PlayerUpdate update : updates) {
            String trimmedRiotId = update.riotId().strip();
            if (!riotIds.add(trimmedRiotId)) {
                throw new IllegalArgumentException("같은 라이엇 ID가 두 줄에 있습니다: " + trimmedRiotId);
            }
        }
        for (Long id : deleteIds) {
            playerRepository.delete(findById(id));
        }
        // 삭제를 먼저 DB에 반영해 둔다. 그래야 지운 사람의 라이엇 ID를 다른 줄이 바로 쓸 수 있다
        playerRepository.flush();
        for (PlayerUpdate update : updates) {
            update(update.id(), update.name(), update.riotId(), update.mainPosition(), update.subPosition(), update.tier());
        }
    }

    /** 경기 기록이 있어서 삭제할 수 없는 플레이어 id */
    public Set<Long> idsWithMatches() {
        return new HashSet<>(matchParticipantRepository.findPlayerIdsWithMatches());
    }

    @Transactional
    public void delete(Long id) {
        Player player = findById(id);
        if (matchParticipantRepository.existsByPlayerId(id)) {
            throw new IllegalStateException("경기 기록이 있는 플레이어는 삭제할 수 없습니다.");
        }
        playerRepository.delete(player);
    }

    public List<Player> findAll(){
        return playerRepository.findAll();
    }

    public List<Player> findAllOrderByName() {
        return playerRepository.findAllByOrderByNameAsc();
    }

    public Player findById(Long id){
        return playerRepository.findById(id).orElseThrow(() -> new NotFoundException("존재하지 않는 플레이어입니다: " + id));
    }
}
