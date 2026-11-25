package com.inhouse.inhouse_manager.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 솔로 랭크 티어. score는 팀 나누기에 쓰는 점수이고, 언랭은 점수가 없다(null).
 * 점수가 없는 사람은 팀 나누기 때 그날 참가자들의 평균 티어 점수로 계산한다.
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
    CHALLENGER("챌린저", 10),
    UNRANKED("언랭", null);

    private final String label;
    private final Integer score;

    /** 평균 점수(예: 4.3)에 가장 가까운 티어 이름. 팀 평균 티어를 보여줄 때 쓴다 */
    public static String nearestLabel(double score) {
        Tier nearest = IRON;
        for (Tier tier : values()) {
            if (tier.score != null && Math.abs(tier.score - score) < Math.abs(nearest.score - score)) {
                nearest = tier;
            }
        }
        return nearest.label;
    }
}
