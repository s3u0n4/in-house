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
        Player player = findActiveById(id);
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
        Set<String> riotIds = new HashSet<>();
        for (PlayerUpdate update : updates) {
            String trimmedRiotId = update.riotId().strip();
            if (!riotIds.add(trimmedRiotId)) {
                throw new IllegalArgumentException("같은 라이엇 ID가 두 줄에 있습니다: " + trimmedRiotId);
            }
        }
        for (Long id : deleteIds) {
            delete(id);
        }
        // 삭제를 먼저 DB에 반영해 둔다. 그래야 지운 사람의 라이엇 ID를 다른 줄이 바로 쓸 수 있다
        playerRepository.flush();
        for (PlayerUpdate update : updates) {
            update(update.id(), update.name(), update.riotId(), update.mainPosition(), update.subPosition(), update.tier());
        }
    }

    /** 경기 기록이 있는 플레이어 id (삭제하면 기록은 남기고 목록에서만 빠지는 사람들) */
    public Set<Long> idsWithMatches() {
        return new HashSet<>(matchParticipantRepository.findPlayerIdsWithMatches());
    }

    /**
     * 플레이어를 삭제한다.
     * 경기 기록이 없으면 DB에서 지우고, 있으면 다른 사람들의 경기 기록과 전적이 망가지지 않도록
     * "삭제됨" 표시만 한다 (목록, 팀 나누기, 경기 기록 선택지에서는 빠진다).
     *
     * @return 경기 기록이 있어서 기록을 남겨 둔 경우 true
     */
    @Transactional
    public boolean delete(Long id) {
        Player player = findActiveById(id);
        if (matchParticipantRepository.existsByPlayerId(id)) {
            player.markDeleted();
            return true;
        }
        playerRepository.delete(player);
        return false;
    }

    /** 삭제되지 않은 플레이어 (등록 순) */
    public List<Player> findAll(){
        return playerRepository.findAllByDeletedAtIsNull();
    }

    /** 삭제되지 않은 플레이어 (이름 순) */
    public List<Player> findAllOrderByName() {
        return playerRepository.findAllByDeletedAtIsNullOrderByNameAsc();
    }

    /** 수정, 삭제할 수 있는 플레이어. 이미 삭제된 플레이어는 없는 것으로 본다 */
    public Player findActiveById(Long id) {
        Player player = findById(id);
        if (player.isDeleted()) {
            throw new NotFoundException("삭제된 플레이어입니다: " + id);
        }
        return player;
    }

    public Player findById(Long id){
        return playerRepository.findById(id).orElseThrow(() -> new NotFoundException("존재하지 않는 플레이어입니다: " + id));
    }
}
