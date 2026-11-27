package com.inhouse.inhouse_manager.player;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 폼의 검증 규칙(@NotNull, @Min, @AssertTrue 등)만 스프링 없이 검사한다
class PlayerFormTest {

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static PlayerForm validForm() {
        PlayerForm form = new PlayerForm();
        form.setName("철수");
        form.setRiotId("철수#KR1");
        form.setMainPosition(Position.MID);
        form.setTopTier(Tier.GOLD);
        form.setJungleTier(Tier.GOLD);
        form.setMidTier(Tier.MASTER);
        form.setAdcTier(Tier.GOLD);
        form.setSupportTier(Tier.GOLD);
        return form;
    }

    private List<String> errorFields(PlayerForm form) {
        return validator.validate(form).stream().map(ConstraintViolation::getPropertyPath).map(Object::toString).toList();
    }

    @Test
    void 올바른_폼은_오류가_없다() {
        assertThat(errorFields(validForm())).isEmpty();
    }

    @Test
    void 부_포지션이_주_포지션과_같으면_오류다() {
        PlayerForm form = validForm();
        form.setSubPosition(Position.MID);

        assertThat(errorFields(form)).containsExactly("subPositionDifferent");
    }

    @Test
    void 마스터_이상_주_라인의_LP는_0부터_3000까지다() {
        PlayerForm form = validForm();
        form.setMainLp(0);
        assertThat(errorFields(form)).isEmpty();
        form.setMainLp(3000);
        assertThat(errorFields(form)).isEmpty();

        form.setMainLp(-1);
        assertThat(errorFields(form)).containsExactly("mainLp");
        form.setMainLp(3001);
        assertThat(errorFields(form)).containsExactly("mainLp");
    }

    @Test
    void 마스터_아래_주_라인은_단계가_필수이고_LP는_0부터_99까지다() {
        PlayerForm form = validForm();
        form.setMidTier(Tier.GOLD);
        assertThat(errorFields(form)).containsExactly("mainDivisionChosen");

        form.setMainDivision(2);
        assertThat(errorFields(form)).isEmpty();
        form.setMainLp(99);
        assertThat(errorFields(form)).isEmpty();

        form.setMainLp(100);
        assertThat(errorFields(form)).containsExactly("mainLpInDivision");
        form.setMainLp(50);
        form.setMainDivision(5);
        assertThat(errorFields(form)).containsExactly("mainDivision");
    }

    @Test
    void 주_라인이_아닌_라인은_티어만_고르면_된다() {
        // 주 라인(미드)은 마스터라서 단계가 없어도 되고, 골드인 다른 라인들은 단계를 묻지 않는다
        assertThat(errorFields(validForm())).isEmpty();
    }
}
