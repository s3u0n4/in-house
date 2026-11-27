package com.inhouse.inhouse_manager.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 티어. score는 팀 나누기에 쓰는 점수다 (티어 1개 = 1점).
 * 주 라인은 세부 점수(LP)까지 적는다 (2026-11-28, scoreWithLp):
 * - 아이언 ~ 다이아몬드: 단계 4 → 3 → 2 → 1 (1이 가장 높다), 단계마다 0~99LP, 100LP를 넘기면 다음 단계.
 *   그래서 티어 안에서 쌓은 LP는 0~399다 (4단계 0LP = 0, 3단계 50LP = 150, 1단계 99LP = 399). 400LP = 티어 1개 = 1점.
 * - 마스터 ~ 챌린저: 0LP부터 이어지는 점수. 0LP 마스터 = 8점, 500LP당 1점 (챌린저 컷이 1000이면 1000LP = 10점).
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
    /** 아이언 ~ 다이아몬드의 단계 수(4 ~ 1)와 한 단계의 LP */
    public static final int DIVISIONS = 4;
    public static final int LP_PER_DIVISION = 100;
    /** 아이언 ~ 다이아몬드에서 티어 1개(1점)의 LP */
    public static final int LP_PER_TIER = DIVISIONS * LP_PER_DIVISION;

    private final String label;
    private final int score;

    /** 마스터, 그랜드마스터, 챌린저 (LP를 적을 수 있는 티어) */
    public boolean isApex() {
        return score >= MASTER.score;
    }

    /**
     * 팀 나누기 점수. lp: 이 티어 안에서 쌓은 LP (없으면 티어 점수 그대로 = 4단계 0LP로 본다).
     * 마스터 이상은 8 + LP / 500, 그 아래는 티어 점수 + LP / 400. 예: 골드 4단계 0LP = 4.0, 골드 2단계 50LP(250) = 4.625
     */
    public double scoreWithLp(Integer lp) {
        if (lp == null) {
            return score;
        }
        if (isApex()) {
            return MASTER.score + lp / LP_PER_SCORE;
        }
        return score + (double) lp / LP_PER_TIER;
    }

    /** 단계(4~1)와 단계 안 LP(0~99)를 티어 안에서 쌓은 LP로. 예: 3단계 50LP → 150 */
    public static int tierLp(int division, int lpInDivision) {
        return (DIVISIONS - division) * LP_PER_DIVISION + lpInDivision;
    }

    /** 티어 안에서 쌓은 LP의 단계. 예: 150 → 3 */
    public static int divisionOf(int tierLp) {
        return DIVISIONS - tierLp / LP_PER_DIVISION;
    }

    /** 티어 안에서 쌓은 LP의 단계 안 LP. 예: 150 → 50 */
    public static int lpInDivisionOf(int tierLp) {
        return tierLp % LP_PER_DIVISION;
    }

    /** 화면에 보여줄 이름. 예: "골드" (세부 없음), "골드 3 50LP", "마스터 350LP" */
    public String labelWithLp(Integer lp) {
        if (lp == null) {
            return label;
        }
        if (isApex()) {
            return label + " " + lp + "LP";
        }
        return label + " " + divisionOf(lp) + " " + lpInDivisionOf(lp) + "LP";
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
