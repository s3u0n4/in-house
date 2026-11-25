package com.inhouse.inhouse_manager.match;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.Position;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate playedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Team winner;

    // mappedBy: 연결 정보(외래 키)는 MatchParticipant.match 쪽이 관리한다는 뜻
    // cascade ALL + orphanRemoval: 경기를 저장/삭제하면 참가 기록도 함께 저장/삭제된다
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchParticipant> participants = new ArrayList<>();

    public Match(LocalDate playedAt, Team winner) {
        this.playedAt = playedAt;
        this.winner = winner;
    }

    public void addParticipant(Player player, Team team, Position position) {
        participants.add(new MatchParticipant(this, player, team, position));
    }

    public List<MatchParticipant> getBlueTeam() {
        return getTeam(Team.BLUE);
    }

    public List<MatchParticipant> getRedTeam() {
        return getTeam(Team.RED);
    }

    private List<MatchParticipant> getTeam(Team team) {
        return participants.stream()
                .filter(participant -> participant.getTeam() == team)
                .sorted(Comparator.comparing(MatchParticipant::getPosition))
                .toList();
    }
}
