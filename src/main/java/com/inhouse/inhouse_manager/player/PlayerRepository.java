package com.inhouse.inhouse_manager.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player,Long> {
    Optional<Player> findByRiotId(String riotId);

    boolean existsByRiotId(String riotId);

    boolean existsByRiotIdAndIdNot(String riotId, Long id);

    // DeletedAtIsNull: 삭제되지 않은 플레이어만, PendingFalse: 승인된 플레이어만
    List<Player> findAllByDeletedAtIsNullAndPendingFalse();

    List<Player> findAllByDeletedAtIsNullAndPendingFalseOrderByNameAsc();

    /** 승인 대기 중인 등록 신청 (먼저 신청한 순) */
    List<Player> findAllByPendingTrueAndDeletedAtIsNullOrderByIdAsc();

    long countByPendingTrueAndDeletedAtIsNull();

    /** 휴지통: 삭제된 플레이어 (최근에 지운 것부터) */
    List<Player> findAllByDeletedAtIsNotNullOrderByDeletedAtDesc();
}
