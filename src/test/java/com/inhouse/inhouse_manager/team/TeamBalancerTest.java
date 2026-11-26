package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.team.TeamBalancer.Assignment;
import com.inhouse.inhouse_manager.team.TeamBalancer.Candidate;
import com.inhouse.inhouse_manager.team.TeamBalancer.TeamSplit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 스프링 없이 순수 자바 객체만 테스트한다 → 빠르다
class TeamBalancerTest {

    TeamBalancer teamBalancer = new TeamBalancer();

    /** 테스트용 후보. 내전 기록이 없는 사람(보정 승률 0.5)으로 만든다 */
    private static Candidate candidate(long id, Position main, double tierScore) {
        return new Candidate(id, "p" + id, main, null, "티어", tierScore, 0.5);
    }

    private static List<Candidate> sameCandidates() {
        List<Candidate> candidates = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            candidates.add(candidate(i, Position.MID, 4));
        }
        return candidates;
    }

    @Test
    void 열명을_다섯명씩_나누고_팀마다_포지션을_하나씩_배치한다() {
        TeamSplit split = teamBalancer.split(sameCandidates());

        assertThat(split.blue().assignments()).extracting(Assignment::position).containsExactly(Position.values());
        assertThat(split.red().assignments()).extracting(Assignment::position).containsExactly(Position.values());
        List<Long> everyone = Stream.concat(split.blue().playerIds().stream(), split.red().playerIds().stream()).toList();
        assertThat(everyone).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    }

    @Test
    void 티어가_높은_두_사람은_서로_다른_팀에_들어간다() {
        List<Candidate> candidates = sameCandidates();
        candidates.set(0, candidate(1, Position.MID, 10));
        candidates.set(1, candidate(2, Position.MID, 10));

        TeamSplit split = teamBalancer.split(candidates);

        assertThat(split.powerDiff()).isLessThan(1e-9);
        assertThat(split.blue().playerIds().contains(1L)).isNotEqualTo(split.blue().playerIds().contains(2L));
    }

    @Test
    void 티어_합이_같아도_분포가_비슷하게_나눈다() {
        // 9, 9, 1, 1, 5×6 → {9,9,1,1,5} 대 {5,5,5,5,5}도 합은 같지만, 9와 1이 한 명씩 가야 분포가 같다
        List<Candidate> candidates = new ArrayList<>();
        double[] tiers = {9, 9, 1, 1, 5, 5, 5, 5, 5, 5};
        for (int i = 0; i < tiers.length; i++) {
            candidates.add(candidate(i + 1, Position.MID, tiers[i]));
        }

        TeamSplit split = teamBalancer.split(candidates);

        assertThat(split.distributionDiff()).isLessThan(1e-9);
        assertThat(split.blue().playerIds()).containsAnyOf(1L, 2L).containsAnyOf(3L, 4L);
        assertThat(split.blue().playerIds().contains(1L)).isNotEqualTo(split.blue().playerIds().contains(2L));
        assertThat(split.blue().playerIds().contains(3L)).isNotEqualTo(split.blue().playerIds().contains(4L));
    }

    @Test
    void 티어가_같으면_내전_승률이_높은_두_사람을_나눈다() {
        List<Candidate> candidates = sameCandidates();
        candidates.set(0, new Candidate(1L, "에이스1", Position.MID, null, "골드", 4, 0.9));
        candidates.set(1, new Candidate(2L, "에이스2", Position.MID, null, "골드", 4, 0.9));

        TeamSplit split = teamBalancer.split(candidates);

        assertThat(split.blue().playerIds().contains(1L)).isNotEqualTo(split.blue().playerIds().contains(2L));
    }

    @Test
    void 실력이_비슷하면_주_포지션이_겹치지_않게_나눈다() {
        // 포지션마다 두 명씩 → 한 팀에 한 명씩 들어가면 모두 주 포지션을 맡을 수 있다
        List<Candidate> candidates = new ArrayList<>();
        long id = 1;
        for (Position position : Position.values()) {
            candidates.add(candidate(id++, position, 4));
            candidates.add(candidate(id++, position, 4));
        }

        TeamSplit split = teamBalancer.split(candidates);

        assertThat(split.blue().assignments()).allMatch(assignment -> assignment.fit() == 2);
        assertThat(split.red().assignments()).allMatch(assignment -> assignment.fit() == 2);
    }

    @Test
    void 주_포지션이_아니면_전력을_깎는다() {
        Candidate top = candidate(1, Position.TOP, 4);
        Candidate topWithSub = new Candidate(2L, "p2", Position.TOP, Position.MID, "골드", 4, 0.5);

        assertThat(new Assignment(top, Position.TOP).power()).isEqualTo(4.0);
        assertThat(new Assignment(topWithSub, Position.MID).power()).isEqualTo(3.0);
        assertThat(new Assignment(top, Position.MID).power()).isEqualTo(2.0);
    }

    @Test
    void 라인이_겹치는_사람은_두_팀에_고르게_나눠서_포지션_감점이_같아지게_한다() {
        // 미드 주포 6명 + 탑·정글·원딜·서폿 1명씩, 모두 골드
        // 미드 3:3이면 두 팀 다 2명이 다른 라인을 가지만, 2:4면 한 팀만 3명이 다른 라인을 가서 약해진다
        List<Candidate> candidates = new ArrayList<>();
        long id = 1;
        for (int i = 0; i < 6; i++) {
            candidates.add(candidate(id++, Position.MID, 4));
        }
        for (Position position : List.of(Position.TOP, Position.JUNGLE, Position.ADC, Position.SUPPORT)) {
            candidates.add(candidate(id++, position, 4));
        }

        TeamSplit split = teamBalancer.split(candidates);

        assertThat(split.blue().positionPenalty()).isEqualTo(split.red().positionPenalty());
        assertThat(split.powerDiff()).isLessThan(1e-9);
    }

    @Test
    void 열명이_아니면_예외가_발생한다() {
        List<Candidate> nine = new ArrayList<>(sameCandidates().subList(0, 9));

        assertThatThrownBy(() -> teamBalancer.split(nine)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 고른_라인의_두_사람만_팀을_맞바꾸고_포지션은_그대로다() {
        TeamSplit split = teamBalancer.split(sameCandidates());

        TeamSplit swapped = teamBalancer.swapPairs(split, Set.of(Position.TOP, Position.MID));

        for (int i = 0; i < 5; i++) {
            Assignment blueBefore = split.blue().assignments().get(i);
            Assignment redBefore = split.red().assignments().get(i);
            Assignment blueAfter = swapped.blue().assignments().get(i);
            Assignment redAfter = swapped.red().assignments().get(i);
            assertThat(blueAfter.position()).isEqualTo(blueBefore.position());
            boolean chosen = blueBefore.position() == Position.TOP || blueBefore.position() == Position.MID;
            assertThat(blueAfter.player()).isEqualTo(chosen ? redBefore.player() : blueBefore.player());
            assertThat(redAfter.player()).isEqualTo(chosen ? blueBefore.player() : redBefore.player());
        }
    }
}
