package com.inhouse.inhouse_manager.player;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String riotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position mainPosition;

    @Enumerated(EnumType.STRING)
    private Position subPosition;


    public Player(String name, String riotId, Position mainPosition, Position subPosition) {
        this.name = name;
        this.riotId = riotId;
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
    }
}
