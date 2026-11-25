package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerRepository;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.stats.StatsService;
import com.inhouse.inhouse_manager.team.TeamBalancer.TeamSplit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TeamService.class, StatsService.class, TeamBalancer.class})
class TeamServiceTest {

    @Autowired
    TeamService teamService;

    @Autowired
    PlayerRepository playerRepository;

    List<Long> ids;

    @BeforeEach
    void setUp() {
        ids = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            ids.add(playerRepository.save(new Player("플레이어" + i, "player#" + i, Position.MID, null)).getId());
        }
    }

    @Test
    void 등록된_플레이어_10명으로_팀을_나눈다() {
        TeamSplit split = teamService.split(ids);

        assertThat(split.blue().playerIds()).hasSize(5);
        assertThat(split.red().playerIds()).hasSize(5);
    }

    @Test
    void 참가자가_10명이_아니면_현재_인원을_알려준다() {
        assertThatThrownBy(() -> teamService.split(ids.subList(0, 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 3명");
    }

    @Test
    void 같은_id를_두_번_보내도_한_명으로_센다() {
        List<Long> withDuplicate = new ArrayList<>(ids.subList(0, 9));
        withDuplicate.add(ids.get(0));

        assertThatThrownBy(() -> teamService.split(withDuplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 9명");
    }
}
