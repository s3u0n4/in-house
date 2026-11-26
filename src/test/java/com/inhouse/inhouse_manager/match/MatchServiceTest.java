package com.inhouse.inhouse_manager.match;

import com.inhouse.inhouse_manager.common.NotFoundException;
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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(MatchService.class)
class MatchServiceTest {

    @Autowired
    MatchService matchService;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    MatchParticipantRepository matchParticipantRepository;

    List<Long> blueIds;
    List<Long> redIds;

    @BeforeEach
    void setUp() {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            ids.add(playerRepository.save(new Player("플레이어" + i, "player#" + i, Position.MID, null, Tier.GOLD, null)).getId());
        }
        blueIds = ids.subList(0, 5);
        redIds = ids.subList(5, 10);
    }

    @Test
    void 경기를_기록하면_10명의_참가_기록이_포지션별로_저장된다() {
        Long matchId = matchService.record(LocalDate.of(2026, 11, 24), Team.BLUE, blueIds, redIds);

        Match match = matchService.findById(matchId);
        assertThat(match.getParticipants()).hasSize(10);
        assertThat(match.getBlueTeam()).extracting(MatchParticipant::getPosition)
                .containsExactly(Position.values());
        assertThat(match.getBlueTeam().get(0).getPlayer().getId()).isEqualTo(blueIds.get(0));
        assertThat(match.getRedTeam().get(4).getPlayer().getId()).isEqualTo(redIds.get(4));
        assertThat(match.getBlueTeam()).allMatch(MatchParticipant::isWin);
        assertThat(match.getRedTeam()).noneMatch(MatchParticipant::isWin);
    }

    @Test
    void 빈_자리가_있으면_예외가_발생한다() {
        List<Long> withEmptySlot = Arrays.asList(blueIds.get(0), blueIds.get(1), null, blueIds.get(3), blueIds.get(4));

        assertThatThrownBy(() -> matchService.record(LocalDate.now(), Team.RED, withEmptySlot, redIds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("모든 자리에");
    }

    @Test
    void 같은_플레이어가_두_자리에_있으면_예외가_발생한다() {
        List<Long> duplicated = new ArrayList<>(redIds);
        duplicated.set(0, blueIds.get(0));

        assertThatThrownBy(() -> matchService.record(LocalDate.now(), Team.RED, blueIds, duplicated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("두 자리에")
                .hasMessageContaining("플레이어1");
    }

    @Test
    void 미래_날짜는_기록할_수_없다() {
        assertThatThrownBy(() -> matchService.record(LocalDate.now().plusDays(1), Team.RED, blueIds, redIds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("미래");
    }

    @Test
    void 경기_목록은_최신_날짜순이다() {
        Long older = matchService.record(LocalDate.of(2026, 11, 1), Team.BLUE, blueIds, redIds);
        Long newer = matchService.record(LocalDate.of(2026, 11, 20), Team.RED, blueIds, redIds);

        assertThat(matchService.findAll()).extracting(Match::getId).containsExactly(newer, older);
    }

    @Test
    void 경기를_삭제하면_목록에서_빠지고_참가_기록은_휴지통용으로_남는다() {
        Long matchId = matchService.record(LocalDate.now(), Team.BLUE, blueIds, redIds);

        matchService.delete(matchId);

        assertThatThrownBy(() -> matchService.findById(matchId)).isInstanceOf(NotFoundException.class);
        assertThat(matchService.findAll()).isEmpty();
        assertThat(matchService.findDeleted()).extracting(Match::getId).containsExactly(matchId);
        assertThat(matchParticipantRepository.count()).isEqualTo(10);
    }

    @Test
    void 관리자는_삭제한_경기를_되돌린다() {
        Long matchId = matchService.record(LocalDate.now(), Team.BLUE, blueIds, redIds);
        matchService.delete(matchId);

        matchService.restore(matchId);

        assertThat(matchService.findById(matchId).getParticipants()).hasSize(10);
        assertThat(matchService.findDeleted()).isEmpty();
    }

    @Test
    void 삭제하지_않은_경기는_되돌릴_수_없다() {
        Long matchId = matchService.record(LocalDate.now(), Team.BLUE, blueIds, redIds);

        assertThatThrownBy(() -> matchService.restore(matchId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 삭제된_플레이어는_새_경기에_넣을_수_없다() {
        Player deleted = playerRepository.findById(blueIds.get(0)).orElseThrow();
        deleted.markDeleted();

        assertThatThrownBy(() -> matchService.record(LocalDate.now(), Team.BLUE, blueIds, redIds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는");
    }
}
