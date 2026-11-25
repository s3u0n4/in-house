package com.inhouse.inhouse_manager.stats;

import com.inhouse.inhouse_manager.match.MatchParticipant;
import com.inhouse.inhouse_manager.player.Position;

import java.util.List;

/**
 * 플레이어 상세 화면에 보여줄 전적 묶음.
 */
public record PlayerRecord(PlayerStats total, List<PositionStats> byPosition, List<MatchParticipant> recentMatches) {

    public record PositionStats(Position position, PlayerStats stats) {
    }
}
