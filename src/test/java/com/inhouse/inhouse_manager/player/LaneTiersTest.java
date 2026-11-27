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
    void 아이언부터_다이아는_단계와_LP로_점수를_매긴다() {
        // 단계 4 → 1 (1이 가장 높다), 단계마다 100LP. 티어 안에서 쌓은 LP로 저장한다: 골드 3 50LP = 150
        assertThat(Tier.tierLp(4, 0)).isZero();
        assertThat(Tier.tierLp(3, 50)).isEqualTo(150);
        assertThat(Tier.tierLp(1, 99)).isEqualTo(399);
        assertThat(Tier.divisionOf(150)).isEqualTo(3);
        assertThat(Tier.lpInDivisionOf(150)).isEqualTo(50);

        // 티어 1개 = 400LP = 1점
        assertThat(Tier.GOLD.scoreWithLp(0)).isEqualTo(4.0);
        assertThat(Tier.GOLD.scoreWithLp(150)).isEqualTo(4.375);
        assertThat(Tier.GOLD.scoreWithLp(null)).isEqualTo(4.0);
        // 다이아 1 99LP는 마스터 0LP 바로 아래
        assertThat(Tier.DIAMOND.scoreWithLp(399)).isLessThan(Tier.MASTER.scoreWithLp(0));
        // 브론즈 1 0LP와 실버 3 0LP는 200점(0.5) 차이
        assertThat(Tier.SILVER.scoreWithLp(Tier.tierLp(3, 0)) - Tier.BRONZE.scoreWithLp(Tier.tierLp(1, 0))).isEqualTo(0.5);
    }

    @Test
    void 단계를_붙여서_보여준다() {
        LaneTiers lanes = new LaneTiers(Tier.GOLD, 150, Tier.GOLD, null, Tier.GOLD, null,
                Tier.GOLD, null, Tier.GOLD, null);

        assertThat(lanes.labelOf(Position.TOP)).isEqualTo("골드 3 50LP");
        assertThat(lanes.labelOf(Position.JUNGLE)).isEqualTo("골드");
    }

    @Test
    void 주_라인의_LP만_남긴다() {
        // 예전에는 마스터 이상이면 어느 라인이든 LP를 적을 수 있었다
        LaneTiers lanes = new LaneTiers(Tier.MASTER, 350, Tier.GOLD, null, Tier.CHALLENGER, 1200,
                Tier.GOLD, null, Tier.GOLD, null);

        LaneTiers kept = lanes.keepLpOnly(Position.MID);

        assertThat(kept.lpOf(Position.MID)).isEqualTo(1200);
        assertThat(kept.lpOf(Position.TOP)).isNull();
        assertThat(kept.labelOf(Position.TOP)).isEqualTo("마스터");
    }

    @Test
    void 화면에는_LP를_붙여서_보여준다() {
        LaneTiers lanes = new LaneTiers(Tier.MASTER, 350, Tier.GOLD, null, Tier.GOLD, null,
                Tier.GOLD, null, Tier.GOLD, null);

        assertThat(lanes.labelOf(Position.TOP)).isEqualTo("마스터 350LP");
        assertThat(lanes.labelOf(Position.JUNGLE)).isEqualTo("골드");
    }

    private static PlayerForm goldForm(Position main) {
        PlayerForm form = new PlayerForm();
        form.setMainPosition(main);
        form.setTopTier(Tier.GOLD);
        form.setJungleTier(Tier.GOLD);
        form.setMidTier(Tier.GOLD);
        form.setAdcTier(Tier.GOLD);
        form.setSupportTier(Tier.GOLD);
        return form;
    }

    @Test
    void 폼에서_주_라인의_단계와_LP를_합쳐서_주_라인에만_저장한다() {
        PlayerForm form = goldForm(Position.MID);
        form.setMainDivision(3);
        form.setMainLp(50);

        LaneTiers lanes = form.toLaneTiers();

        assertThat(lanes.midLp()).isEqualTo(150);
        assertThat(lanes.topLp()).isNull();
        assertThat(lanes.labelOf(Position.MID)).isEqualTo("골드 3 50LP");

        // 수정 화면에서는 다시 단계와 LP로 나눠서 채운다
        PlayerForm filled = goldForm(Position.MID);
        filled.fillLaneTiers(lanes);
        assertThat(filled.getMainDivision()).isEqualTo(3);
        assertThat(filled.getMainLp()).isEqualTo(50);
    }

    @Test
    void 폼에서_LP를_비우면_0LP다() {
        PlayerForm form = goldForm(Position.MID);
        form.setMainDivision(1);

        assertThat(form.toLaneTiers().midLp()).isEqualTo(300);
    }

    @Test
    void 폼에서_마스터_이상은_LP를_그대로_저장한다() {
        PlayerForm form = goldForm(Position.TOP);
        form.setTopTier(Tier.GRANDMASTER);
        form.setMainLp(600);

        LaneTiers lanes = form.toLaneTiers();

        assertThat(lanes.topLp()).isEqualTo(600);
        assertThat(lanes.labelOf(Position.TOP)).isEqualTo("그랜드마스터 600LP");
    }
}
