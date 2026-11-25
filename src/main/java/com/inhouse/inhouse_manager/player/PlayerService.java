package com.inhouse.inhouse_manager.player;

import com.inhouse.inhouse_manager.common.NotFoundException;
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

    /**
     * 플레이어를 삭제한다. DB에서 지우지 않고 "삭제됨" 표시만 한다.
     * 목록, 팀 나누기, 경기 기록 선택지에서는 빠지고, 지난 경기 기록에는 "(삭제됨)"으로 남는다.
     * 관리자가 휴지통에서 되돌릴 수 있다.
     */
    @Transactional
    public void delete(Long id) {
        findActiveById(id).markDeleted();
    }

    /** 휴지통에 있는 플레이어 */
    public List<Player> findDeleted() {
        return playerRepository.findAllByDeletedAtIsNotNullOrderByDeletedAtDesc();
    }

    /** 관리자 전용: 삭제한 플레이어를 되돌린다. 그사이 같은 라이엇 ID로 등록한 사람이 있으면 되돌릴 수 없다 */
    @Transactional
    public void restore(Long id) {
        Player player = findById(id);
        if (!player.isDeleted()) {
            throw new NotFoundException("휴지통에 없는 플레이어입니다: " + id);
        }
        String originalRiotId = player.getOriginalRiotId();
        if (playerRepository.existsByRiotId(originalRiotId)) {
            throw new IllegalStateException(player.getName() + " 님을 되돌릴 수 없습니다. 라이엇 ID " + originalRiotId
                    + "를 쓰는 플레이어가 이미 있어요. 그 플레이어의 라이엇 ID를 먼저 바꿔 주세요.");
        }
        player.restore();
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
