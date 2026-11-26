package com.inhouse.inhouse_manager.ranking;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerService;
import com.inhouse.inhouse_manager.stats.PlayerStats;
import com.inhouse.inhouse_manager.stats.StatsService;
import com.inhouse.inhouse_manager.team.TeamBalancer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {
    private final PlayerService playerService;
    private final StatsService statsService;

    /**
     * 삭제되지 않은 플레이어의 랭킹. 순위에 드는 사람을 먼저 정렬해서 순위를 매기고,
     * 순위에 들지 않는 사람(승률·다승은 경기 없는 사람, 전력은 라인별 티어가 없는 사람)은 이름 순으로 뒤에 붙인다.
     */
    public List<RankingRow> ranking(RankingSort sort) {
        List<Player> players = playerService.findAll();
        Map<Long, PlayerStats> stats = statsService.statsByPlayer(players.stream().map(Player::getId).toList());

        List<RankingRow> ranked = new ArrayList<>();
        List<RankingRow> unranked = new ArrayList<>();
        for (Player player : players) {
            RankingRow row = new RankingRow(null, player, stats.get(player.getId()), powerOf(player, stats.get(player.getId())));
            if (isRanked(row, sort)) {
                ranked.add(row);
            } else {
                unranked.add(row);
            }
        }

        // 같은 점수 안에서는 경기 수가 많은 쪽, 그다음 이름 순으로 줄을 세운다 (순위는 같다)
        ranked.sort(Comparator.comparingLong((RankingRow row) -> score(row, sort)).reversed()
                .thenComparing(row -> row.stats().games(), Comparator.reverseOrder())
                .thenComparing(row -> row.player().getName()));
        unranked.sort(Comparator.comparing(row -> row.player().getName()));

        List<RankingRow> result = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            RankingRow row = ranked.get(i);
            // 바로 앞 사람과 점수가 같으면 같은 순위, 다르면 (앞에 있는 사람 수 + 1)위
            int rank = i > 0 && score(row, sort) == score(ranked.get(i - 1), sort)
                    ? result.get(i - 1).rank() : i + 1;
            result.add(new RankingRow(rank, row.player(), row.stats(), row.power()));
        }
        result.addAll(unranked);
        return result;
    }

    private static boolean isRanked(RankingRow row, RankingSort sort) {
        return sort == RankingSort.POWER ? row.power() != null : row.stats().games() > 0;
    }

    /**
     * 정렬과 순위에 쓰는 점수. 화면에 보이는 자릿수로 반올림해서 비교한다
     * (안 그러면 화면에는 둘 다 66.7%인데 순위가 다르게 나올 수 있다).
     * 승률은 보정 승률이 같으면 이긴 경기 수로 한 번 더 가른다.
     */
    private static long score(RankingRow row, RankingSort sort) {
        return switch (sort) {
            case RATE -> Math.round(row.stats().rating() * 1000) * 1000L + row.stats().wins();
            case WINS -> row.stats().wins() * 1_000_000L + Math.round(row.stats().rating() * 1000);
            case POWER -> Math.round(row.power() * 10);
        };
    }

    /** 전력 = 주 포지션 라인 티어 점수 + 내전 성적 보정. 팀 나누기와 같은 식이다 */
    private static Double powerOf(Player player, PlayerStats stats) {
        if (!player.hasLaneTiers()) {
            return null;
        }
        return player.getLaneTiers().scoreOf(player.getMainPosition()) + TeamBalancer.ratingBonus(stats.rating());
    }
}
