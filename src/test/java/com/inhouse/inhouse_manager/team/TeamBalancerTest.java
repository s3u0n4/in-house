package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.team.TeamBalancer.Assignment;
import com.inhouse.inhouse_manager.team.TeamBalancer.Candidate;
import com.inhouse.inhouse_manager.team.TeamBalancer.TeamSplit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 스프링 없이 순수 자바 객체만 테스트한다 → 빠르다
class TeamBalancerTest {

    TeamBalancer teamBalancer = new TeamBalancer();

    @Test
    void 열명을_다섯명씩_나누고_팀마다_포지션을_하나씩_배치한다() {
        List<Candidate> candidates = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            candidates.add(new Candidate(i, "p" + i, Position.MID, null, 0.5));
        }

        TeamSplit split = teamBalancer.split(candidates);

        assertThat(split.blue().assignments()).extracting(Assignment::position).containsExactly(Position.values());
        assertThat(split.red().assignments()).extracting(Assignment::position).containsExactly(Position.values());
        List<Long> everyone = Stream.concat(split.blue().playerIds().stream(), split.red().playerIds().stream()).toList();
        assertThat(everyone).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    }

    @Test
    void 잘하는_두_사람은_서로_다른_팀에_들어간다() {
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(new Candidate(1L, "에이스1", Position.MID, null, 0.9));
        candidates.add(new Candidate(2L, "에이스2", Position.MID, null, 0.9));
        for (long i = 3; i <= 10; i++) {
            candidates.add(new Candidate(i, "p" + i, Position.MID, null, 0.5));
        }

        TeamSplit split = teamBalancer.split(candidates);

        assertThat(split.scoreDiff()).isLessThan(1e-9);
        assertThat(split.blue().playerIds().contains(1L)).isNotEqualTo(split.blue().playerIds().contains(2L));
    }

    @Test
    void 실력이_비슷하면_주_포지션이_겹치지_않게_나눈다() {
        // 포지션마다 두 명씩 → 한 팀에 한 명씩 들어가면 모두 주 포지션을 맡을 수 있다
        List<Candidate> candidates = new ArrayList<>();
        long id = 1;
        for (Position position : Position.values()) {
            candidates.add(new Candidate(id, "p" + id, position, null, 0.5));
            id++;
            candidates.add(new Candidate(id, "p" + id, position, null, 0.5));
            id++;
        }

        TeamSplit split = teamBalancer.split(candidates);

        assertThat(split.blue().assignments()).allMatch(assignment -> assignment.fit() == 2);
        assertThat(split.red().assignments()).allMatch(assignment -> assignment.fit() == 2);
    }

    @Test
    void 열명이_아니면_예외가_발생한다() {
        List<Candidate> nine = new ArrayList<>();
        for (long i = 1; i <= 9; i++) {
            nine.add(new Candidate(i, "p" + i, Position.MID, null, 0.5));
        }

        assertThatThrownBy(() -> teamBalancer.split(nine)).isInstanceOf(IllegalArgumentException.class);
    }
}
