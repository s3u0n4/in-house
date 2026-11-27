package com.inhouse.inhouse_manager.player;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * 라인별 티어: 본인이 생각하기에 그 라인을 어느 티어 정도로 하는지. 팀 나누기에 쓴다.
 * 2026-11-28부터 주 라인만 세부 LP를 적는다: 아이언~다이아는 티어 안에서 쌓은 LP(0~399, 단계로 바꿔 보여준다),
 * 마스터 이상은 LP. 나머지 라인은 티어만 (LP 칸은 null). 자세한 규칙은 Tier 참고.
 * 그 전에는 마스터 이상이면 어느 라인이든 LP를 적을 수 있었다 → Player.getLaneTiers()가 주 라인 것만 남긴다.
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

    /** 그 라인의 티어 안 LP. 안 적었으면 null */
    public Integer lpOf(Position position) {
        if (of(position) == null) {
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

    /** 팀 나누기 점수 (세부 LP를 적은 라인은 LP 반영) */
    public double scoreOf(Position position) {
        return of(position).scoreWithLp(lpOf(position));
    }

    /** 화면에 보여줄 이름. 예: "다이아몬드", "골드 3 50LP", "마스터 350LP" */
    public String labelOf(Position position) {
        return of(position).labelWithLp(lpOf(position));
    }

    /** mainLane의 LP만 남기고 나머지 라인의 LP는 지운 복사본 (주 라인만 세부 점수를 쓴다) */
    public LaneTiers keepLpOnly(Position mainLane) {
        return new LaneTiers(top, mainLane == Position.TOP ? topLp : null,
                jungle, mainLane == Position.JUNGLE ? jungleLp : null,
                mid, mainLane == Position.MID ? midLp : null,
                adc, mainLane == Position.ADC ? adcLp : null,
                support, mainLane == Position.SUPPORT ? supportLp : null);
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
