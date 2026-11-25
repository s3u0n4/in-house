package com.inhouse.inhouse_manager.match;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    // join fetch: 경기와 참가자, 플레이어를 쿼리 한 번에 같이 읽는다 (N+1 쿼리 방지)
    @Query("select distinct m from Match m left join fetch m.participants p left join fetch p.player " +
            "order by m.playedAt desc, m.id desc")
    List<Match> findAllWithParticipants();

    @Query("select m from Match m left join fetch m.participants p left join fetch p.player where m.id = :id")
    Optional<Match> findWithParticipantsById(Long id);
}
