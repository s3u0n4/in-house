package com.inhouse.inhouse_manager.stats;

import com.inhouse.inhouse_manager.match.MatchService;
import com.inhouse.inhouse_manager.match.Team;
import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerRepository;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.player.Tier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({StatsService.class, MatchService.class})
class StatsServiceTest {

    @Autowired
    StatsService statsService;

    @Autowired
    MatchService matchService;

    @Autowired
    PlayerRepository playerRepository;

    List<Long> ids;

    @BeforeEach
    void setUp() {
        ids = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            ids.add(playerRepository.save(new Player("플레이어" + i, "player#" + i, Position.MID, null, null)).getId());
        }
    }

    @Test
    void 플레이어별_승패와_승률을_계산한다() {
        // 0번 플레이어: 1경기는 블루 TOP으로 이기고, 1경기는 레드 TOP으로 진다
        matchService.record(LocalDate.of(2026, 11, 1), Team.BLUE, ids.subList(0, 5), ids.subList(5, 10));
        matchService.record(LocalDate.of(2026, 11, 2), Team.BLUE, ids.subList(5, 10), ids.subList(0, 5));

        Map<Long, PlayerStats> stats = statsService.statsByPlayer(ids);

        PlayerStats first = stats.get(ids.get(0));
        assertThat(first.games()).isEqualTo(2);
        assertThat(first.wins()).isEqualTo(1);
        assertThat(first.losses()).isEqualTo(1);
        assertThat(first.winRateText()).isEqualTo("50%");
    }

    @Test
    void 경기가_없는_플레이어는_빈_전적이고_레이팅은_0_5다() {
        Map<Long, PlayerStats> stats = statsService.statsByPlayer(ids);

        PlayerStats noGames = stats.get(ids.get(10));
        assertThat(noGames.games()).isZero();
        assertThat(noGames.winRateText()).isEqualTo("-");
        assertThat(noGames.rating()).isEqualTo(0.5);
    }

    @Test
    void 상세_전적은_포지션별로_나누고_최근_경기부터_보여준다() {
        // 0번 플레이어: 블루 TOP 승 → 레드 TOP 패 → 블루 TOP 승, 3번 플레이어는 ADC
        matchService.record(LocalDate.of(2026, 11, 1), Team.BLUE, ids.subList(0, 5), ids.subList(5, 10));
        matchService.record(LocalDate.of(2026, 11, 2), Team.BLUE, ids.subList(5, 10), ids.subList(0, 5));
        matchService.record(LocalDate.of(2026, 11, 3), Team.BLUE, ids.subList(0, 5), ids.subList(5, 10));

        PlayerRecord record = statsService.recordOf(ids.get(0));

        assertThat(record.total().games()).isEqualTo(3);
        assertThat(record.total().wins()).isEqualTo(2);
        assertThat(record.byPosition()).hasSize(1);
        assertThat(record.byPosition().get(0).position()).isEqualTo(Position.TOP);
        assertThat(record.recentMatches()).extracting(participant -> participant.getMatch().getPlayedAt())
                .containsExactly(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1));
    }

    @Test
    void 삭제한_경기는_전적에서_빠진다() {
        Long first = matchService.record(LocalDate.of(2026, 11, 1), Team.BLUE, ids.subList(0, 5), ids.subList(5, 10));
        matchService.record(LocalDate.of(2026, 11, 2), Team.RED, ids.subList(0, 5), ids.subList(5, 10));

        matchService.delete(first);

        PlayerStats stats = statsService.statsByPlayer(ids).get(ids.get(0));
        assertThat(stats.games()).isEqualTo(1);
        assertThat(stats.wins()).isZero();
        assertThat(statsService.recordOf(ids.get(0)).recentMatches()).hasSize(1);
    }
}
