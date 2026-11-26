package com.inhouse.inhouse_manager.ranking;

import com.inhouse.inhouse_manager.match.Match;
import com.inhouse.inhouse_manager.match.MatchRepository;
import com.inhouse.inhouse_manager.match.Team;
import com.inhouse.inhouse_manager.player.LaneTiers;
import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerRepository;
import com.inhouse.inhouse_manager.player.PlayerService;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.player.Tier;
import com.inhouse.inhouse_manager.stats.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({RankingService.class, PlayerService.class, StatsService.class})
class RankingServiceTest {

    @Autowired
    RankingService rankingService;

    @Autowired
    PlayerService playerService;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    MatchRepository matchRepository;

    private Player player(String name, Tier mainLaneTier) {
        LaneTiers lanes = mainLaneTier == null ? null
                : new LaneTiers(Tier.IRON, Tier.IRON, mainLaneTier, Tier.IRON, Tier.IRON);
        return playerRepository.save(new Player(name, name + "#KR", Position.MID, null, lanes));
    }

    /** player가 블루팀 미드로 나와 이기거나 진 경기를 wins번, losses번 넣는다 (다른 참가자는 생략) */
    private void record(Player player, int wins, int losses) {
        for (int i = 0; i < wins + losses; i++) {
            Match match = new Match(LocalDate.of(2026, 11, 1), i < wins ? Team.BLUE : Team.RED);
            match.addParticipant(player, Team.BLUE, Position.MID);
            matchRepository.save(match);
        }
    }

    private List<String> names(List<RankingRow> rows) {
        return rows.stream().map(row -> row.player().getName()).toList();
    }

    private List<Integer> ranks(List<RankingRow> rows) {
        return rows.stream().map(RankingRow::rank).toList();
    }

    @Test
    void 승률_순위는_보정_승률_순이라_1전_1승이_4전_4승보다_아래다() {
        record(player("한판", Tier.GOLD), 1, 0);      // 보정 2/3 = 67%
        record(player("네판", Tier.GOLD), 4, 0);      // 보정 5/6 = 83%
        record(player("반반", Tier.GOLD), 2, 2);      // 보정 3/6 = 50%

        List<RankingRow> rows = rankingService.ranking(RankingSort.RATE);

        assertThat(names(rows)).containsExactly("네판", "한판", "반반");
        assertThat(ranks(rows)).containsExactly(1, 2, 3);
        assertThat(rows.get(0).ratingText()).isEqualTo("83%");
    }

    @Test
    void 기록이_같으면_같은_순위이고_다음_순위는_건너뛴다() {
        record(player("가", Tier.GOLD), 3, 1);
        record(player("나", Tier.GOLD), 3, 1);
        record(player("다", Tier.GOLD), 1, 3);

        List<RankingRow> rows = rankingService.ranking(RankingSort.RATE);

        assertThat(ranks(rows)).containsExactly(1, 1, 3);
    }

    @Test
    void 경기가_없는_사람은_순위_없이_맨_뒤에_이름_순으로() {
        player("하", Tier.GOLD);
        player("가", Tier.GOLD);
        record(player("나", Tier.GOLD), 0, 5);

        List<RankingRow> rows = rankingService.ranking(RankingSort.RATE);

        assertThat(names(rows)).containsExactly("나", "가", "하");
        assertThat(ranks(rows)).containsExactly(1, null, null);
    }

    @Test
    void 다승은_이긴_경기_수_순이다() {
        record(player("적게많이", Tier.GOLD), 2, 0);
        record(player("많이많이", Tier.GOLD), 5, 5);

        List<RankingRow> rows = rankingService.ranking(RankingSort.WINS);

        assertThat(names(rows)).containsExactly("많이많이", "적게많이");
    }

    @Test
    void 전력은_주_라인_티어와_내전_보정이고_라인별_티어가_없으면_순위에서_빠진다() {
        player("다이아", Tier.DIAMOND);                 // 7 + 0
        record(player("골드에이스", Tier.GOLD), 8, 0);  // 4 + (9/10 - 0.5) × 4 = 5.6
        player("옛날사람", null);

        List<RankingRow> rows = rankingService.ranking(RankingSort.POWER);

        assertThat(names(rows)).containsExactly("다이아", "골드에이스", "옛날사람");
        assertThat(rows.get(1).power()).isCloseTo(5.6, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(rows.get(2).rank()).isNull();
        assertThat(rows.get(2).power()).isNull();
    }

    @Test
    void 삭제된_플레이어와_삭제된_경기는_빠진다() {
        Player gone = player("삭제됨", Tier.GOLD);
        record(gone, 3, 0);
        playerService.delete(gone.getId());
        Player stay = player("남음", Tier.GOLD);
        record(stay, 1, 0);
        Match deletedMatch = new Match(LocalDate.of(2026, 11, 2), Team.BLUE);
        deletedMatch.addParticipant(stay, Team.BLUE, Position.MID);
        matchRepository.save(deletedMatch).markDeleted();

        List<RankingRow> rows = rankingService.ranking(RankingSort.RATE);

        assertThat(names(rows)).containsExactly("남음");
        assertThat(rows.get(0).stats().games()).isEqualTo(1);
    }
}
