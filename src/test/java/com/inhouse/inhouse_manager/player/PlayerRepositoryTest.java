package com.inhouse.inhouse_manager.player;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PlayerRepositoryTest {

    @Autowired
    PlayerRepository playerRepository;

    @Test
    void 플레이어를_저장하고_라이엇ID로_조회한다(){
        // given: 준비
        Player player = new Player("철수", "철수#KR1", Position.MID, Position.TOP);

        // when: 실행
        playerRepository.save(player);
        Optional<Player> found = playerRepository.findByRiotId("철수#KR1");

        // then: 검증
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("철수");
        assertThat(found.get().getMainPosition()).isEqualTo(Position.MID);
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void 없는_라이엇ID는_false를_반환한다(){
        assertThat(playerRepository.existsByRiotId("없는사람#KR1")).isFalse();
    }
}
