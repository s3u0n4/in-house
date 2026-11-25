package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerRepository;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.player.Tier;
import com.inhouse.inhouse_manager.stats.StatsService;
import com.inhouse.inhouse_manager.team.TeamBalancer.Assignment;
import com.inhouse.inhouse_manager.team.TeamBalancer.Candidate;
import com.inhouse.inhouse_manager.team.TeamBalancer.TeamSplit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
            ids.add(playerRepository.save(new Player("플레이어" + i, "player#" + i, Position.MID, null, Tier.GOLD)).getId());
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

    @Test
    void 언랭과_티어_미입력은_참가자_평균_티어로_계산한다() {
        // 아이언(1점) 4명 + 다이아몬드(7점) 4명 → 평균 4점. 언랭 1명, 미입력(null) 1명
        List<Long> mixed = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            mixed.add(playerRepository.save(new Player("아이언" + i, "iron#" + i, Position.TOP, null, Tier.IRON)).getId());
            mixed.add(playerRepository.save(new Player("다이아" + i, "dia#" + i, Position.MID, null, Tier.DIAMOND)).getId());
        }
        Long unranked = playerRepository.save(new Player("언랭", "unranked#1", Position.ADC, null, Tier.UNRANKED)).getId();
        Long noTier = playerRepository.save(new Player("미입력", "notier#1", Position.SUPPORT, null, null)).getId();
        mixed.add(unranked);
        mixed.add(noTier);

        TeamSplit split = teamService.split(mixed);

        List<Assignment> all = Stream.concat(split.blue().assignments().stream(), split.red().assignments().stream()).toList();
        Candidate unrankedCandidate = all.stream().map(Assignment::player)
                .filter(candidate -> candidate.playerId().equals(unranked)).findFirst().orElseThrow();
        Candidate noTierCandidate = all.stream().map(Assignment::player)
                .filter(candidate -> candidate.playerId().equals(noTier)).findFirst().orElseThrow();
        assertThat(unrankedCandidate.tierScore()).isEqualTo(4.0);
        assertThat(unrankedCandidate.tierLabel()).isEqualTo("언랭");
        assertThat(noTierCandidate.tierScore()).isEqualTo(4.0);
        assertThat(noTierCandidate.tierLabel()).isEqualTo("미입력");
    }
}
