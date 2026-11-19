package com.inhouse.inhouse_manager.player;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
@Import(PlayerService.class)
public class PlayerServiceTest {

    @Autowired
    PlayerService playerService;

    @Test
    void 플레이어를_등록한다(){
        Long id = playerService.register("철수","철수#KR1", Position.MID, Position.TOP);

        Player found = playerService.findById(id);
        assertThat(found.getRiotId()).isEqualTo("철수#KR1");
    }

    @Test
    void 같은_라이엇ID로_등록하면_예외가_발생한다(){
        playerService.register("철수", "철수#KR1", Position.MID, null);

        assertThatThrownBy(() -> playerService.register("영희", "철수#KR1", Position.TOP, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된");
    }

    @Test
    void 없는_id로_조회하면_예외가_발생한다(){
        assertThatThrownBy(() -> playerService.findById(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
