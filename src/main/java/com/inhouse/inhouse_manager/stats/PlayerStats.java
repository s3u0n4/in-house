package com.inhouse.inhouse_manager.stats;

/**
 * 경기 수와 승리 수로 만드는 전적. DB에 저장하지 않고 참가 기록으로 매번 계산한다.
 */
public record PlayerStats(int games, int wins) {
    public static final PlayerStats EMPTY = new PlayerStats(0, 0);

    public int losses() {
        return games - wins;
    }

    public String winRateText() {
        if (games == 0) {
            return "-";
        }
        return Math.round(wins * 100.0 / games) + "%";
    }

    /** 팀 나누기용 보정 승률. 경기가 없으면 0.5에서 시작한다. */
    public double rating() {
        return (wins + 1.0) / (games + 2.0);
    }

    PlayerStats plus(boolean win) {
        return new PlayerStats(games + 1, wins + (win ? 1 : 0));
    }
}
