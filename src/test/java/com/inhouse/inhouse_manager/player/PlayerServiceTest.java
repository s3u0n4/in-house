package com.inhouse.inhouse_manager.player;

import com.inhouse.inhouse_manager.common.NotFoundException;
import com.inhouse.inhouse_manager.match.Match;
import com.inhouse.inhouse_manager.match.MatchRepository;
import com.inhouse.inhouse_manager.match.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(PlayerService.class)
public class PlayerServiceTest {

    @Autowired
    PlayerService playerService;

    @Autowired
    MatchRepository matchRepository;

    @Test
    void 플레이어를_등록한다(){
        Long id = playerService.register("철수","철수#KR1", Position.MID, Position.TOP, Tier.GOLD);

        Player found = playerService.findById(id);
        assertThat(found.getRiotId()).isEqualTo("철수#KR1");
        assertThat(found.getTier()).isEqualTo(Tier.GOLD);
    }

    @Test
    void 등록할_때_앞뒤_공백을_지운다(){
        Long id = playerService.register("  철수 ", " 철수#KR1  ", Position.MID, null, Tier.GOLD);

        Player found = playerService.findById(id);
        assertThat(found.getName()).isEqualTo("철수");
        assertThat(found.getRiotId()).isEqualTo("철수#KR1");
    }

    @Test
    void 같은_라이엇ID로_등록하면_예외가_발생한다(){
        playerService.register("철수", "철수#KR1", Position.MID, null, Tier.GOLD);

        assertThatThrownBy(() -> playerService.register("영희", "철수#KR1", Position.TOP, null, Tier.GOLD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된");
    }

    @Test
    void 없는_id로_조회하면_예외가_발생한다(){
        assertThatThrownBy(() -> playerService.findById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 플레이어_정보를_수정한다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, Tier.GOLD);

        playerService.update(id, "철수2", "철수#KR2", Position.TOP, Position.JUNGLE, Tier.DIAMOND);

        Player found = playerService.findById(id);
        assertThat(found.getName()).isEqualTo("철수2");
        assertThat(found.getRiotId()).isEqualTo("철수#KR2");
        assertThat(found.getMainPosition()).isEqualTo(Position.TOP);
        assertThat(found.getSubPosition()).isEqualTo(Position.JUNGLE);
        assertThat(found.getTier()).isEqualTo(Tier.DIAMOND);
    }

    @Test
    void 자기_라이엇ID를_그대로_두고_수정할_수_있다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, Tier.GOLD);

        playerService.update(id, "철수2", "철수#KR1", Position.MID, null, Tier.GOLD);

        assertThat(playerService.findById(id).getName()).isEqualTo("철수2");
    }

    @Test
    void 다른_플레이어의_라이엇ID로_수정하면_예외가_발생한다(){
        playerService.register("영희", "영희#KR1", Position.TOP, null, Tier.GOLD);
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, Tier.GOLD);

        assertThatThrownBy(() -> playerService.update(id, "철수", "영희#KR1", Position.MID, null, Tier.GOLD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된");
    }

    @Test
    void 경기_기록이_없는_플레이어는_삭제할_수_있다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, Tier.GOLD);

        playerService.delete(id);

        assertThatThrownBy(() -> playerService.findById(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 경기_기록이_있는_플레이어는_삭제할_수_없다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, Tier.GOLD);
        Match match = new Match(LocalDate.now(), Team.BLUE);
        match.addParticipant(playerService.findById(id), Team.BLUE, Position.MID);
        matchRepository.save(match);

        assertThatThrownBy(() -> playerService.delete(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("경기 기록이 있는");
    }

    @Test
    void 여러_명을_한_번에_수정한다(){
        Long a = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        Long b = playerService.register("영희", "영희#KR1", Position.TOP, null, null);

        playerService.updateAll(List.of(
                new PlayerUpdate(a, "철수", "철수#KR1", Position.MID, Position.TOP, Tier.DIAMOND),
                new PlayerUpdate(b, "영희2", "영희#KR2", Position.JUNGLE, null, Tier.SILVER)));

        assertThat(playerService.findById(a).getTier()).isEqualTo(Tier.DIAMOND);
        assertThat(playerService.findById(a).getSubPosition()).isEqualTo(Position.TOP);
        assertThat(playerService.findById(b).getName()).isEqualTo("영희2");
        assertThat(playerService.findById(b).getRiotId()).isEqualTo("영희#KR2");
        assertThat(playerService.findById(b).getTier()).isEqualTo(Tier.SILVER);
    }

    @Test
    void 여러_명_수정에서_같은_라이엇ID가_두_줄에_있으면_아무것도_바꾸지_않는다(){
        Long a = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        Long b = playerService.register("영희", "영희#KR1", Position.TOP, null, null);

        assertThatThrownBy(() -> playerService.updateAll(List.of(
                new PlayerUpdate(a, "철수2", "같은#KR1", Position.MID, null, Tier.GOLD),
                new PlayerUpdate(b, "영희", " 같은#KR1 ", Position.TOP, null, Tier.GOLD))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("두 줄");

        assertThat(playerService.findById(a).getName()).isEqualTo("철수");
    }

    @Test
    void 목록_저장에서_체크한_플레이어를_삭제하고_나머지는_수정한다(){
        Long a = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        Long b = playerService.register("영희", "영희#KR1", Position.TOP, null, Tier.GOLD);

        // 철수를 지우면서 영희가 철수의 라이엇 ID를 가져간다 → 삭제가 먼저 반영돼야 한다
        playerService.saveAll(List.of(new PlayerUpdate(b, "영희", "철수#KR1", Position.TOP, null, Tier.GOLD)), List.of(a));

        assertThatThrownBy(() -> playerService.findById(a)).isInstanceOf(NotFoundException.class);
        assertThat(playerService.findById(b).getRiotId()).isEqualTo("철수#KR1");
    }

    @Test
    void 목록_저장에서_경기_기록이_있는_플레이어를_지우려_하면_아무것도_바꾸지_않는다(){
        Long a = playerService.register("철수", "철수#KR1", Position.MID, null, Tier.GOLD);
        Long b = playerService.register("영희", "영희#KR1", Position.TOP, null, Tier.GOLD);
        Match match = new Match(LocalDate.now(), Team.BLUE);
        match.addParticipant(playerService.findById(a), Team.BLUE, Position.MID);
        matchRepository.save(match);

        assertThatThrownBy(() -> playerService.saveAll(
                List.of(new PlayerUpdate(b, "영희2", "영희#KR1", Position.TOP, null, Tier.GOLD)), List.of(a)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("철수");

        assertThat(playerService.findById(b).getName()).isEqualTo("영희");
        assertThat(playerService.idsWithMatches()).containsExactly(a);
    }
}
