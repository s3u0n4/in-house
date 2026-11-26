package com.inhouse.inhouse_manager.player;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 스프링 없이 순수 자바 객체만 테스트한다
class LaneTiersTest {

    @Test
    void 마스터_이상은_LP를_적으면_8_더하기_LP_나누기_500점이다() {
        assertThat(Tier.MASTER.scoreWithLp(0)).isEqualTo(8.0);
        assertThat(Tier.MASTER.scoreWithLp(250)).isEqualTo(8.5);
        assertThat(Tier.GRANDMASTER.scoreWithLp(500)).isEqualTo(9.0);
        assertThat(Tier.CHALLENGER.scoreWithLp(1500)).isEqualTo(11.0);
    }

    @Test
    void LP를_비우면_티어_점수_그대로다() {
        assertThat(Tier.MASTER.scoreWithLp(null)).isEqualTo(8.0);
        assertThat(Tier.GRANDMASTER.scoreWithLp(null)).isEqualTo(9.0);
        assertThat(Tier.CHALLENGER.scoreWithLp(null)).isEqualTo(10.0);
    }

    @Test
    void 마스터_아래_티어는_LP가_있어도_무시한다() {
        assertThat(Tier.DIAMOND.scoreWithLp(900)).isEqualTo(7.0);

        LaneTiers lanes = new LaneTiers(Tier.DIAMOND, 900, Tier.GOLD, null, Tier.GOLD, null,
                Tier.GOLD, null, Tier.GOLD, null);
        assertThat(lanes.lpOf(Position.TOP)).isNull();
        assertThat(lanes.labelOf(Position.TOP)).isEqualTo("다이아몬드");
    }

    @Test
    void 화면에는_LP를_붙여서_보여준다() {
        LaneTiers lanes = new LaneTiers(Tier.MASTER, 350, Tier.GOLD, null, Tier.GOLD, null,
                Tier.GOLD, null, Tier.GOLD, null);

        assertThat(lanes.labelOf(Position.TOP)).isEqualTo("마스터 350LP");
        assertThat(lanes.labelOf(Position.JUNGLE)).isEqualTo("골드");
    }

    @Test
    void 폼에서_마스터_아래_티어에_적힌_LP는_버린다() {
        PlayerForm form = new PlayerForm();
        form.setTopTier(Tier.GRANDMASTER);
        form.setTopLp(600);
        form.setJungleTier(Tier.GOLD);
        form.setJungleLp(600);
        form.setMidTier(Tier.GOLD);
        form.setAdcTier(Tier.GOLD);
        form.setSupportTier(Tier.GOLD);

        LaneTiers lanes = form.toLaneTiers();

        assertThat(lanes.topLp()).isEqualTo(600);
        assertThat(lanes.jungleLp()).isNull();
    }
}
