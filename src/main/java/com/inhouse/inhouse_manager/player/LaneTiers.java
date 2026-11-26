package com.inhouse.inhouse_manager.player;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * 라인별 티어: 본인이 생각하기에 그 라인을 어느 티어 정도로 하는지. 팀 나누기에 쓴다.
 * Player 테이블 안에 컬럼 5개(top_tier ...)로 들어간다 (@Embeddable).
 * 라인별 티어 기능 전에 등록한 플레이어는 다섯 칸이 모두 비어 있어서, Player.laneTiers가 null이다.
 */
@Embeddable
public record LaneTiers(
        @Enumerated(EnumType.STRING) @Column(name = "top_tier") Tier top,
        @Enumerated(EnumType.STRING) @Column(name = "jungle_tier") Tier jungle,
        @Enumerated(EnumType.STRING) @Column(name = "mid_tier") Tier mid,
        @Enumerated(EnumType.STRING) @Column(name = "adc_tier") Tier adc,
        @Enumerated(EnumType.STRING) @Column(name = "support_tier") Tier support) {

    public Tier of(Position position) {
        return switch (position) {
            case TOP -> top;
            case JUNGLE -> jungle;
            case MID -> mid;
            case ADC -> adc;
            case SUPPORT -> support;
        };
    }

    /** 다섯 칸이 모두 점수가 있는 티어로 채워졌는지 (언랭이나 빈칸이 있으면 팀 나누기에 쓰지 않는다) */
    public boolean isComplete() {
        for (Position position : Position.values()) {
            Tier tier = of(position);
            if (tier == null || tier.getScore() == null) {
                return false;
            }
        }
        return true;
    }
}
