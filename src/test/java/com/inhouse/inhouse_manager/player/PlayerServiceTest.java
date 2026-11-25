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
}
