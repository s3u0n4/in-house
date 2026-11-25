package com.inhouse.inhouse_manager.match;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {
    boolean existsByPlayerId(Long playerId);

    @Query("select distinct mp.player.id from MatchParticipant mp")
    List<Long> findPlayerIdsWithMatches();

    @Query("select mp from MatchParticipant mp join fetch mp.match")
    List<MatchParticipant> findAllWithMatch();

    @Query("select mp from MatchParticipant mp join fetch mp.match m where mp.player.id = :playerId " +
            "order by m.playedAt desc, m.id desc")
    List<MatchParticipant> findAllByPlayerIdWithMatch(Long playerId);
}
