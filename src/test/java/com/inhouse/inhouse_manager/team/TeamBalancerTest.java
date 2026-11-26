package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.LaneTiers;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.player.Tier;
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

    private static Assignment assignmentAt(TeamBalancer.Lineup lineup, Position position) {
        return lineup.assignments().stream().filter(a -> a.position() == position).findFirst().orElseThrow();
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
    void 라인이_겹치면_부_포지션이_되는_사람을_보낸다() {
        // 탑·정글·원딜 주포 2명씩 + 미드 주포 4명, 서폿 주포는 없다
        // 미드 주포 중 2명(7, 8번)만 부 포지션이 서폿이다
        List<Candidate> candidates = new ArrayList<>();
        long id = 1;
        for (Position position : List.of(Position.TOP, Position.JUNGLE, Position.ADC)) {
            candidates.add(candidate(id++, position, 4));
            candidates.add(candidate(id++, position, 4));
        }
        candidates.add(new Candidate(id++, "p7", Position.MID, Position.SUPPORT, "골드", 4, 0.5));
        candidates.add(new Candidate(id++, "p8", Position.MID, Position.SUPPORT, "골드", 4, 0.5));
        candidates.add(candidate(id++, Position.MID, 4));
        candidates.add(candidate(id, Position.MID, 4));

        TeamSplit split = teamBalancer.split(candidates);

        // 팀마다 서폿 부포인 사람이 서폿을 가고, 나머지는 모두 주 포지션
        for (var lineup : List.of(split.blue(), split.red())) {
            Assignment support = assignmentAt(lineup, Position.SUPPORT);
            assertThat(support.player().playerId()).isIn(7L, 8L);
            assertThat(support.fit()).isEqualTo(1);
            assertThat(support.fitLabel()).isEqualTo("부");
            assertThat(lineup.assignments()).filteredOn(a -> a.position() != Position.SUPPORT)
                    .allMatch(a -> a.fit() == 2);
            assertThat(lineup.positionPenalty()).isEqualTo(1.0);
            assertThat(lineup.power()).isEqualTo(5 * 4 - 1.0);
        }
        assertThat(split.powerDiff()).isLessThan(1e-9);
    }

    @Test
    void 주_포지션도_부_포지션도_아닌_라인에_가면_2점을_깎는다() {
        // 탑·정글·원딜 주포 2명씩 + 미드 주포 4명(부 포지션 없음), 서폿을 할 수 있는 사람이 없다
        List<Candidate> candidates = new ArrayList<>();
        long id = 1;
        for (Position position : List.of(Position.TOP, Position.JUNGLE, Position.ADC)) {
            candidates.add(candidate(id++, position, 4));
            candidates.add(candidate(id++, position, 4));
        }
        for (int i = 0; i < 4; i++) {
            candidates.add(candidate(id++, Position.MID, 4));
        }

        TeamSplit split = teamBalancer.split(candidates);

        // 미드 주포가 팀마다 2명씩 가고, 그중 1명이 서폿을 맡는다
        for (var lineup : List.of(split.blue(), split.red())) {
            Assignment support = assignmentAt(lineup, Position.SUPPORT);
            assertThat(support.player().mainPosition()).isEqualTo(Position.MID);
            assertThat(support.fit()).isEqualTo(0);
            assertThat(support.fitLabel()).isEqualTo("-");
            assertThat(lineup.positionPenalty()).isEqualTo(2.0);
            assertThat(lineup.power()).isEqualTo(5 * 4 - 2.0);
        }
        assertThat(split.powerDiff()).isLessThan(1e-9);
    }

    @Test
    void 다른_라인을_가야_하면_부_포지션인_사람을_먼저_보낸다() {
        // 서폿 주포가 없고 미드 주포가 4명. 미드 주포 중 1명(7번)만 부 포지션이 서폿이다
        // 7번이 있는 팀은 7번이 서폿(부, -1)을 가야 하고, 다른 미드 주포를 보내면(-2) 안 된다
        List<Candidate> candidates = new ArrayList<>();
        long id = 1;
        for (Position position : List.of(Position.TOP, Position.JUNGLE, Position.ADC)) {
            candidates.add(candidate(id++, position, 4));
            candidates.add(candidate(id++, position, 4));
        }
        candidates.add(new Candidate(id++, "p7", Position.MID, Position.SUPPORT, "골드", 4, 0.5));
        for (int i = 0; i < 3; i++) {
            candidates.add(candidate(id++, Position.MID, 4));
        }

        TeamSplit split = teamBalancer.split(candidates);

        var teamWithSeven = split.blue().playerIds().contains(7L) ? split.blue() : split.red();
        Assignment support = assignmentAt(teamWithSeven, Position.SUPPORT);
        assertThat(support.player().playerId()).isEqualTo(7L);
        assertThat(support.fit()).isEqualTo(1);
    }

    @Test
    void 다른_라인_가는_사람을_줄이는_것보다_두_팀_감점을_같게_하는_것을_먼저_본다() {
        // 탑·정글·원딜 주포 2명씩, 미드 주포 3명(7번은 부 포지션 서폿), 서폿 주포 1명
        // 한 팀만 7번이 서폿(-1)을 가면 실력 차이가 1 생긴다 → 두 팀 감점이 같은 쪽(-2 대 -2)을 고른다 (2026-11-26 사용자 결정)
        List<Candidate> candidates = new ArrayList<>();
        long id = 1;
        for (Position position : List.of(Position.TOP, Position.JUNGLE, Position.ADC)) {
            candidates.add(candidate(id++, position, 4));
            candidates.add(candidate(id++, position, 4));
        }
        candidates.add(new Candidate(id++, "p7", Position.MID, Position.SUPPORT, "골드", 4, 0.5));
        candidates.add(candidate(id++, Position.MID, 4));
        candidates.add(candidate(id++, Position.MID, 4));
        candidates.add(candidate(id, Position.SUPPORT, 4));

        TeamSplit split = teamBalancer.split(candidates);

        assertThat(split.powerDiff()).isLessThan(1e-9);
        assertThat(split.blue().positionPenalty()).isEqualTo(split.red().positionPenalty());
    }

    /** 한 라인만 다이아(7)이고 나머지는 브론즈(2)인 사람. 주 포지션은 모두 미드로 둔다 */
    private static Candidate laneSpecialist(long id, Position strongLane) {
        Tier[] tiers = new Tier[5];
        for (Position position : Position.values()) {
            tiers[position.ordinal()] = position == strongLane ? Tier.DIAMOND : Tier.BRONZE;
        }
        LaneTiers lanes = new LaneTiers(tiers[0], tiers[1], tiers[2], tiers[3], tiers[4]);
        return new Candidate(id, "p" + id, Position.MID, null, "골드", 4, 0.5, lanes);
    }

    @Test
    void 라인별_티어가_있으면_맡은_라인의_티어로_전력을_계산한다() {
        Candidate topDiamond = laneSpecialist(1, Position.TOP);

        Assignment atTop = new Assignment(topDiamond, Position.TOP);
        Assignment atMid = new Assignment(topDiamond, Position.MID);

        assertThat(atTop.power()).isEqualTo(7.0);
        assertThat(atTop.tierLabel()).isEqualTo("다이아몬드");
        // 주 포지션(미드)이어도 미드 라인 티어(브론즈)로 계산하고, 포지션 감점은 없다
        assertThat(atMid.power()).isEqualTo(2.0);
        assertThat(atMid.positionPenalty()).isEqualTo(0.0);
    }

    @Test
    void 라인별_티어가_있으면_주_포지션보다_잘하는_라인에_배치한다() {
        // 라인마다 그 라인만 다이아인 사람이 두 명씩. 주 포지션은 모두 미드다
        List<Candidate> candidates = new ArrayList<>();
        long id = 1;
        for (Position position : Position.values()) {
            candidates.add(laneSpecialist(id++, position));
            candidates.add(laneSpecialist(id++, position));
        }

        TeamSplit split = teamBalancer.split(candidates);

        // 팀마다 다섯 명 모두 자기가 다이아인 라인을 맡는다 → 두 팀 다 전력 35
        for (var lineup : List.of(split.blue(), split.red())) {
            assertThat(lineup.assignments()).allMatch(a -> a.tierScore() == 7.0);
            assertThat(lineup.power()).isEqualTo(35.0);
        }
        assertThat(split.powerDiff()).isLessThan(1e-9);
    }

    @Test
    void 라인별_티어가_있는_사람과_없는_사람을_같이_나눈다() {
        // 라인별 티어가 있는 5명(라인마다 한 명씩 다이아) + 없는 5명(라인마다 한 명씩 주포, 솔랭 다이아)
        List<Candidate> candidates = new ArrayList<>();
        long id = 1;
        for (Position position : Position.values()) {
            candidates.add(laneSpecialist(id++, position));
            candidates.add(candidate(id++, position, 7));
        }

        TeamSplit split = teamBalancer.split(candidates);

        // 모두 자기가 잘하는 라인(다이아)을 맡으면 두 팀 다 35, 감점 없음
        assertThat(split.powerDiff()).isLessThan(1e-9);
        assertThat(split.blue().power()).isEqualTo(35.0);
        assertThat(split.blue().positionPenalty() + split.red().positionPenalty()).isEqualTo(0.0);
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
