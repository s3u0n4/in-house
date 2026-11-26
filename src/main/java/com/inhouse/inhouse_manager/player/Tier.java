package com.inhouse.inhouse_manager.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 티어. score는 팀 나누기에 쓰는 점수다.
 * 마스터 이상은 LP를 적으면 LP로 점수를 계산한다 (scoreWithLp).
 */
@Getter
@RequiredArgsConstructor
public enum Tier {
    IRON("아이언", 1),
    BRONZE("브론즈", 2),
    SILVER("실버", 3),
    GOLD("골드", 4),
    PLATINUM("플래티넘", 5),
    EMERALD("에메랄드", 6),
    DIAMOND("다이아몬드", 7),
    MASTER("마스터", 8),
    GRANDMASTER("그랜드마스터", 9),
    CHALLENGER("챌린저", 10);

    /** 마스터 이상에서 LP 몇 점이 티어 1단계인지. 0LP = 8점(마스터), 500LP = 9점, 1000LP = 10점 */
    static final double LP_PER_SCORE = 500.0;

    private final String label;
    private final int score;

    /** 마스터, 그랜드마스터, 챌린저 (LP를 적을 수 있는 티어) */
    public boolean isApex() {
        return score >= MASTER.score;
    }

    /** 팀 나누기 점수. 마스터 이상이고 LP를 적었으면 8 + LP / 500, 아니면 티어 점수 */
    public double scoreWithLp(Integer lp) {
        if (isApex() && lp != null) {
            return MASTER.score + lp / LP_PER_SCORE;
        }
        return score;
    }

    /** 평균 점수(예: 4.3)에 가장 가까운 티어 이름. 팀 평균 티어를 보여줄 때 쓴다 */
    public static String nearestLabel(double score) {
        Tier nearest = IRON;
        for (Tier tier : values()) {
            if (Math.abs(tier.score - score) < Math.abs(nearest.score - score)) {
                nearest = tier;
            }
        }
        return nearest.label;
    }
}
