package com.inhouse.inhouse_manager.match;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {
    // 전적은 삭제되지 않은 경기만으로 계산한다
    @Query("select mp from MatchParticipant mp join fetch mp.match m where m.deletedAt is null")
    List<MatchParticipant> findAllWithMatch();

    @Query("select mp from MatchParticipant mp join fetch mp.match m where mp.player.id = :playerId " +
            "and m.deletedAt is null order by m.playedAt desc, m.id desc")
    List<MatchParticipant> findAllByPlayerIdWithMatch(Long playerId);
}
