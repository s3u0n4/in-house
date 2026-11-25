package com.inhouse.inhouse_manager.match;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.Position;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "player_id"}),
        @UniqueConstraint(columnNames = {"match_id", "team", "position"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 참가 기록 여러 개가 경기 하나에 속한다 (N:1). LAZY: 실제로 쓸 때 DB에서 읽는다
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    MatchParticipant(Match match, Player player, Team team, Position position) {
        this.match = match;
        this.player = player;
        this.team = team;
        this.position = position;
    }

    public boolean isWin() {
        return team == match.getWinner();
    }
}
