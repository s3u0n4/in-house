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

    // 티어 기능 전에 등록한 플레이어는 값이 없어서 DB에서는 null을 허용한다 (새로 등록할 때는 폼에서 필수)
    @Enumerated(EnumType.STRING)
    private Tier tier;

    public Player(String name, String riotId, Position mainPosition, Position subPosition, Tier tier) {
        this.name = name;
        this.riotId = riotId;
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
        this.tier = tier;
    }

    public void update(String name, String riotId, Position mainPosition, Position subPosition, Tier tier) {
        this.name = name;
        this.riotId = riotId;
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
        this.tier = tier;
    }
}
