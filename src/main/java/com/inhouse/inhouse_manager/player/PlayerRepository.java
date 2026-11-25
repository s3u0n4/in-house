package com.inhouse.inhouse_manager.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player,Long> {
    Optional<Player> findByRiotId(String riotId);

    boolean existsByRiotId(String riotId);

    boolean existsByRiotIdAndIdNot(String riotId, Long id);

    // DeletedAtIsNull: 삭제되지 않은 플레이어만
    List<Player> findAllByDeletedAtIsNull();

    List<Player> findAllByDeletedAtIsNullOrderByNameAsc();
}
