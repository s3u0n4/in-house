package com.inhouse.inhouse_manager.player;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * 라인별 티어: 본인이 생각하기에 그 라인을 어느 티어 정도로 하는지. 팀 나누기에 쓴다.
 * 마스터 이상은 LP도 적을 수 있다 (선택). 마스터 아래 티어의 LP는 저장하지 않는다.
 * Player 테이블 안에 컬럼 10개(top_tier, top_lp ...)로 들어간다 (@Embeddable).
 * 라인별 티어 기능 전에 등록한 플레이어는 모든 칸이 비어 있어서, Player.laneTiers가 null이다.
 */
@Embeddable
public record LaneTiers(
        @Enumerated(EnumType.STRING) @Column(name = "top_tier") Tier top,
        @Column(name = "top_lp") Integer topLp,
        @Enumerated(EnumType.STRING) @Column(name = "jungle_tier") Tier jungle,
        @Column(name = "jungle_lp") Integer jungleLp,
        @Enumerated(EnumType.STRING) @Column(name = "mid_tier") Tier mid,
        @Column(name = "mid_lp") Integer midLp,
        @Enumerated(EnumType.STRING) @Column(name = "adc_tier") Tier adc,
        @Column(name = "adc_lp") Integer adcLp,
        @Enumerated(EnumType.STRING) @Column(name = "support_tier") Tier support,
        @Column(name = "support_lp") Integer supportLp) {

    /** LP 없이 티어만 */
    public LaneTiers(Tier top, Tier jungle, Tier mid, Tier adc, Tier support) {
        this(top, null, jungle, null, mid, null, adc, null, support, null);
    }

    public Tier of(Position position) {
        return switch (position) {
            case TOP -> top;
            case JUNGLE -> jungle;
            case MID -> mid;
            case ADC -> adc;
            case SUPPORT -> support;
        };
    }

    /** 그 라인의 LP. 마스터 아래 티어이거나 안 적었으면 null */
    public Integer lpOf(Position position) {
        Tier tier = of(position);
        if (tier == null || !tier.isApex()) {
            return null;
        }
        return switch (position) {
            case TOP -> topLp;
            case JUNGLE -> jungleLp;
            case MID -> midLp;
            case ADC -> adcLp;
            case SUPPORT -> supportLp;
        };
    }

    /** 팀 나누기 점수 (마스터 이상은 LP 반영) */
    public double scoreOf(Position position) {
        return of(position).scoreWithLp(lpOf(position));
    }

    /** 화면에 보여줄 이름. 예: "다이아몬드", "마스터 350LP" */
    public String labelOf(Position position) {
        Integer lp = lpOf(position);
        return lp == null ? of(position).getLabel() : of(position).getLabel() + " " + lp + "LP";
    }

    /** 다섯 라인 모두 티어가 있는지 (빈칸이 있으면 팀 나누기에 쓰지 않는다) */
    public boolean isComplete() {
        for (Position position : Position.values()) {
            if (of(position) == null) {
                return false;
            }
        }
        return true;
    }
}
