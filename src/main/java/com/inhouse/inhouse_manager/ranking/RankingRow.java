package com.inhouse.inhouse_manager.ranking;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.stats.PlayerStats;

/**
 * 랭킹 표의 한 줄.
 * rank: 순위. 같은 기록이면 같은 순위(1, 1, 3). 순위에 들지 않는 사람(경기 없음, 전력 계산 불가)은 null
 * power: 전력 (주 포지션 라인 티어 + 내전 보정). 라인별 티어가 없으면 null
 */
public record RankingRow(Integer rank, Player player, PlayerStats stats, Double power) {

    /** 보정 승률을 퍼센트 글자로. 예: "67%" */
    public String ratingText() {
        return Math.round(stats.rating() * 100) + "%";
    }

    /** 1~3위 (화면에서 강조) */
    public boolean isPodium() {
        return rank != null && rank <= 3;
    }
}
