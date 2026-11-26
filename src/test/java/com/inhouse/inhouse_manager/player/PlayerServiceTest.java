package com.inhouse.inhouse_manager.player;

import com.inhouse.inhouse_manager.common.NotFoundException;
import com.inhouse.inhouse_manager.match.Match;
import com.inhouse.inhouse_manager.match.MatchRepository;
import com.inhouse.inhouse_manager.match.Team;
import jakarta.persistence.EntityManager;
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

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlayerRepository playerRepository;

    static final LaneTiers LANES = new LaneTiers(Tier.SILVER, Tier.BRONZE, Tier.PLATINUM, Tier.GOLD, Tier.IRON);

    @Test
    void 라인별_티어를_저장하고_다시_읽는다() {
        Long id = playerService.register("철수", "철수#KR1", Position.MID, Position.TOP, LANES);
        // 메모리에 남은 객체가 아니라 DB에서 다시 읽어 오게 비운다
        entityManager.flush();
        entityManager.clear();

        Player found = playerService.findById(id);
        assertThat(found.getLaneTiers()).isEqualTo(LANES);
        assertThat(found.getLaneTiers().of(Position.MID)).isEqualTo(Tier.PLATINUM);
        assertThat(found.hasLaneTiers()).isTrue();
    }

    @Test
    void 등록_신청은_승인_전까지_목록에_나오지_않는다() {
        Long approved = playerService.register("철수", "철수#KR1", Position.MID, null, LANES);
        Long requested = playerService.requestRegistration("영희", "영희#KR1", Position.TOP, null, LANES);

        assertThat(playerService.findAll()).extracting(Player::getId).containsExactly(approved);
        assertThat(playerService.findPending()).extracting(Player::getId).containsExactly(requested);
        assertThat(playerService.countPending()).isEqualTo(1);
        assertThatThrownBy(() -> playerService.findVisibleById(requested)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> playerService.findActiveById(requested)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 승인하면_목록에_나온다() {
        Long requested = playerService.requestRegistration("영희", "영희#KR1", Position.TOP, null, LANES);

        playerService.approve(requested);

        assertThat(playerService.findAll()).extracting(Player::getId).containsExactly(requested);
        assertThat(playerService.findPending()).isEmpty();
    }

    @Test
    void 거절하면_지워지고_같은_라이엇ID로_다시_신청할_수_있다() {
        Long requested = playerService.requestRegistration("영희", "영희#KR1", Position.TOP, null, LANES);

        playerService.reject(requested);

        assertThat(playerRepository.findById(requested)).isEmpty();
        assertThat(playerService.requestRegistration("영희", "영희#KR1", Position.TOP, null, LANES)).isNotNull();
    }

    @Test
    void 승인_대기_중인_라이엇ID로는_등록할_수_없다() {
        playerService.requestRegistration("영희", "영희#KR1", Position.TOP, null, LANES);

        assertThatThrownBy(() -> playerService.register("다른영희", "영희#KR1", Position.TOP, null, LANES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("승인 대기");
    }

    @Test
    void 이미_승인된_플레이어는_승인하거나_거절할_수_없다() {
        Long approved = playerService.register("철수", "철수#KR1", Position.MID, null, LANES);

        assertThatThrownBy(() -> playerService.approve(approved)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> playerService.reject(approved)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 마스터_이상의_LP도_저장한다() {
        LaneTiers withLp = new LaneTiers(Tier.MASTER, 350, Tier.GOLD, null, Tier.CHALLENGER, 1200,
                Tier.GOLD, null, Tier.GOLD, null);
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, withLp);
        entityManager.flush();
        entityManager.clear();

        LaneTiers found = playerService.findById(id).getLaneTiers();
        assertThat(found.lpOf(Position.TOP)).isEqualTo(350);
        assertThat(found.labelOf(Position.MID)).isEqualTo("챌린저 1200LP");
        assertThat(found.scoreOf(Position.MID)).isEqualTo(8 + 1200 / 500.0);
    }

    @Test
    void 라인별_티어가_없던_플레이어에게_나중에_넣을_수_있다() {
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        entityManager.flush();
        entityManager.clear();
        assertThat(playerService.findById(id).hasLaneTiers()).isFalse();

        playerService.update(id, "철수", "철수#KR1", Position.MID, null, LANES);
        entityManager.flush();
        entityManager.clear();

        assertThat(playerService.findById(id).getLaneTiers()).isEqualTo(LANES);
    }

    @Test
    void 플레이어를_등록한다(){
        Long id = playerService.register("철수","철수#KR1", Position.MID, Position.TOP, null);

        Player found = playerService.findById(id);
        assertThat(found.getRiotId()).isEqualTo("철수#KR1");
    }

    @Test
    void 등록할_때_앞뒤_공백을_지운다(){
        Long id = playerService.register("  철수 ", " 철수#KR1  ", Position.MID, null, null);

        Player found = playerService.findById(id);
        assertThat(found.getName()).isEqualTo("철수");
        assertThat(found.getRiotId()).isEqualTo("철수#KR1");
    }

    @Test
    void 같은_라이엇ID로_등록하면_예외가_발생한다(){
        playerService.register("철수", "철수#KR1", Position.MID, null, null);

        assertThatThrownBy(() -> playerService.register("영희", "철수#KR1", Position.TOP, null, null))
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
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);

        playerService.update(id, "철수2", "철수#KR2", Position.TOP, Position.JUNGLE, LANES);

        Player found = playerService.findById(id);
        assertThat(found.getName()).isEqualTo("철수2");
        assertThat(found.getRiotId()).isEqualTo("철수#KR2");
        assertThat(found.getMainPosition()).isEqualTo(Position.TOP);
        assertThat(found.getSubPosition()).isEqualTo(Position.JUNGLE);
        assertThat(found.getLaneTiers()).isEqualTo(LANES);
    }

    @Test
    void 자기_라이엇ID를_그대로_두고_수정할_수_있다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);

        playerService.update(id, "철수2", "철수#KR1", Position.MID, null, null);

        assertThat(playerService.findById(id).getName()).isEqualTo("철수2");
    }

    @Test
    void 다른_플레이어의_라이엇ID로_수정하면_예외가_발생한다(){
        playerService.register("영희", "영희#KR1", Position.TOP, null, null);
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);

        assertThatThrownBy(() -> playerService.update(id, "철수", "영희#KR1", Position.MID, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된");
    }

    @Test
    void 경기_기록이_없는_플레이어도_삭제_표시만_하고_목록에서_뺀다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);

        playerService.delete(id);

        assertThat(playerService.findById(id).isDeleted()).isTrue();
        assertThat(playerService.findAll()).extracting(Player::getId).doesNotContain(id);
        assertThatThrownBy(() -> playerService.findActiveById(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 경기_기록이_있는_플레이어를_삭제하면_목록에서만_빠지고_기록은_남는다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        Match match = new Match(LocalDate.now(), Team.BLUE);
        match.addParticipant(playerService.findById(id), Team.BLUE, Position.MID);
        matchRepository.save(match);

        playerService.delete(id);

        assertThat(playerService.findById(id).isDeleted()).isTrue();
        assertThat(playerService.findById(id).getDisplayName()).isEqualTo("철수 (삭제됨)");
        assertThat(playerService.findAll()).extracting(Player::getId).doesNotContain(id);
        assertThat(playerService.findAllOrderByName()).extracting(Player::getId).doesNotContain(id);
        assertThat(matchRepository.findWithParticipantsById(match.getId()).orElseThrow().getParticipants()).hasSize(1);
        assertThatThrownBy(() -> playerService.findActiveById(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 삭제된_플레이어의_라이엇ID로_다시_등록할_수_있다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        Match match = new Match(LocalDate.now(), Team.BLUE);
        match.addParticipant(playerService.findById(id), Team.BLUE, Position.MID);
        matchRepository.save(match);
        playerService.delete(id);

        Long again = playerService.register("철수", "철수#KR1", Position.MID, null, null);

        assertThat(again).isNotEqualTo(id);
        assertThat(playerService.findById(again).getRiotId()).isEqualTo("철수#KR1");
    }

    @Test
    void 여러_명을_한_번에_수정한다(){
        Long a = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        Long b = playerService.register("영희", "영희#KR1", Position.TOP, null, null);

        playerService.updateAll(List.of(
                new PlayerUpdate(a, "철수", "철수#KR1", Position.MID, Position.TOP, LANES),
                new PlayerUpdate(b, "영희2", "영희#KR2", Position.JUNGLE, null, null)));

        assertThat(playerService.findById(a).getLaneTiers()).isEqualTo(LANES);
        assertThat(playerService.findById(a).getSubPosition()).isEqualTo(Position.TOP);
        assertThat(playerService.findById(b).getName()).isEqualTo("영희2");
        assertThat(playerService.findById(b).getRiotId()).isEqualTo("영희#KR2");
        assertThat(playerService.findById(b).getLaneTiers()).isNull();
    }

    @Test
    void 여러_명_수정에서_같은_라이엇ID가_두_줄에_있으면_아무것도_바꾸지_않는다(){
        Long a = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        Long b = playerService.register("영희", "영희#KR1", Position.TOP, null, null);

        assertThatThrownBy(() -> playerService.updateAll(List.of(
                new PlayerUpdate(a, "철수2", "같은#KR1", Position.MID, null, null),
                new PlayerUpdate(b, "영희", " 같은#KR1 ", Position.TOP, null, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("두 줄");

        assertThat(playerService.findById(a).getName()).isEqualTo("철수");
    }

    @Test
    void 목록_저장에서_체크한_플레이어를_삭제하고_나머지는_수정한다(){
        Long a = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        Long b = playerService.register("영희", "영희#KR1", Position.TOP, null, null);

        // 철수를 지우면서 영희가 철수의 라이엇 ID를 가져간다 → 삭제가 먼저 반영돼야 한다
        playerService.saveAll(List.of(new PlayerUpdate(b, "영희", "철수#KR1", Position.TOP, null, null)), List.of(a));

        assertThat(playerService.findById(a).isDeleted()).isTrue();
        assertThat(playerService.findById(b).getRiotId()).isEqualTo("철수#KR1");
    }

    @Test
    void 목록_저장에서_경기_기록이_있는_플레이어는_기록을_남기고_삭제한다(){
        Long a = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        Long b = playerService.register("영희", "영희#KR1", Position.TOP, null, null);
        Match match = new Match(LocalDate.now(), Team.BLUE);
        match.addParticipant(playerService.findById(a), Team.BLUE, Position.MID);
        matchRepository.save(match);

        playerService.saveAll(List.of(new PlayerUpdate(b, "영희2", "철수#KR1", Position.TOP, null, null)), List.of(a));

        assertThat(playerService.findById(a).isDeleted()).isTrue();
        assertThat(playerService.findById(b).getName()).isEqualTo("영희2");
        assertThat(playerService.findById(b).getRiotId()).isEqualTo("철수#KR1");
    }

    @Test
    void 관리자는_삭제한_플레이어를_라이엇ID까지_원래대로_되돌린다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        playerService.delete(id);

        assertThat(playerService.findDeleted()).extracting(Player::getId).containsExactly(id);
        assertThat(playerService.findById(id).getOriginalRiotId()).isEqualTo("철수#KR1");

        playerService.restore(id);

        Player restored = playerService.findById(id);
        assertThat(restored.isDeleted()).isFalse();
        assertThat(restored.getRiotId()).isEqualTo("철수#KR1");
        assertThat(playerService.findAll()).extracting(Player::getId).contains(id);
        assertThat(playerService.findDeleted()).isEmpty();
    }

    @Test
    void 그사이_같은_라이엇ID로_등록한_사람이_있으면_되돌리지_않는다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);
        playerService.delete(id);
        playerService.register("새철수", "철수#KR1", Position.TOP, null, null);

        assertThatThrownBy(() -> playerService.restore(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("철수#KR1");
        assertThat(playerService.findById(id).isDeleted()).isTrue();
    }

    @Test
    void 삭제하지_않은_플레이어는_되돌릴_수_없다(){
        Long id = playerService.register("철수", "철수#KR1", Position.MID, null, null);

        assertThatThrownBy(() -> playerService.restore(id)).isInstanceOf(NotFoundException.class);
    }
}
