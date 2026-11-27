package com.inhouse.inhouse_manager.player;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerForm {
    /** LP 입력 범위. 한국 서버 챌린저 1등도 대개 이 안에 든다 */
    static final int MAX_LP = 3000;

    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 30, message = "이름은 30자 이하로 입력하세요.")
    private String name;

    @NotBlank(message = "라이엇 ID를 입력하세요.")
    @Size(max = 50, message = "라이엇 ID는 50자 이하로 입력하세요.")
    private String riotId;

    @NotNull(message = "주 포지션을 선택하세요.")
    private Position mainPosition;
    private Position subPosition;

    /** 부 포지션은 없거나 주 포지션과 달라야 한다 */
    @AssertTrue(message = "부 포지션은 주 포지션과 다르게 골라 주세요.")
    public boolean isSubPositionDifferent() {
        return subPosition == null || subPosition != mainPosition;
    }

    // 라인별 티어: 본인이 생각하기에 그 라인을 어느 티어 정도로 하는지 (팀 나누기에 쓴다). 다섯 칸 모두 티어만 고른다
    @NotNull(message = "탑 티어를 선택하세요.")
    private Tier topTier;
    @NotNull(message = "정글 티어를 선택하세요.")
    private Tier jungleTier;
    @NotNull(message = "미드 티어를 선택하세요.")
    private Tier midTier;
    @NotNull(message = "원딜 티어를 선택하세요.")
    private Tier adcTier;
    @NotNull(message = "서폿 티어를 선택하세요.")
    private Tier supportTier;

    // 주 라인(주 포지션의 라인)만 최고 티어를 자세히 적는다 (2026-11-28). 규칙은 Tier 참고
    // 아이언 ~ 다이아: 단계 1~4 (필수, 1이 가장 높다) + LP 0~99 (비우면 0). 마스터 이상: LP 0~3000 (선택)
    @Min(value = 1, message = "단계는 1~4 중에서 골라 주세요.") @Max(value = Tier.DIVISIONS, message = "단계는 1~4 중에서 골라 주세요.")
    private Integer mainDivision;
    @Min(value = 0, message = "LP는 0 이상으로 입력하세요.") @Max(value = MAX_LP, message = "LP는 3000 이하로 입력하세요.")
    private Integer mainLp;

    /** 주 라인이 아이언 ~ 다이아면 단계를 꼭 골라야 한다 */
    @AssertTrue(message = "주 라인 티어의 단계(1~4)를 골라 주세요.")
    public boolean isMainDivisionChosen() {
        Tier tier = mainLaneTier();
        return tier == null || tier.isApex() || mainDivision != null;
    }

    /** 아이언 ~ 다이아의 LP는 한 단계 안의 점수라서 0~99 (100LP가 되면 다음 단계) */
    @AssertTrue(message = "마스터 아래 티어의 LP는 0~99로 입력하세요. (100LP가 되면 다음 단계예요)")
    public boolean isMainLpInDivision() {
        Tier tier = mainLaneTier();
        return tier == null || tier.isApex() || mainLp == null || mainLp < Tier.LP_PER_DIVISION;
    }

    /** 입력칸 값으로 LaneTiers를 만든다. 세부 LP는 주 라인에만 넣는다 */
    public LaneTiers toLaneTiers() {
        Integer lp = mainTierLp();
        return new LaneTiers(topTier, lpAt(Position.TOP, lp), jungleTier, lpAt(Position.JUNGLE, lp),
                midTier, lpAt(Position.MID, lp), adcTier, lpAt(Position.ADC, lp),
                supportTier, lpAt(Position.SUPPORT, lp));
    }

    private Integer lpAt(Position lane, Integer lp) {
        return lane == mainPosition ? lp : null;
    }

    /** 주 라인의 티어 안 LP. 마스터 이상은 LP 그대로, 그 아래는 단계와 LP를 합친 값 (예: 3단계 50LP → 150) */
    private Integer mainTierLp() {
        Tier tier = mainLaneTier();
        if (tier == null) {
            return null;
        }
        if (tier.isApex()) {
            return mainLp;
        }
        if (mainDivision == null) {
            return null;
        }
        return Tier.tierLp(mainDivision, mainLp == null ? 0 : mainLp);
    }

    private Tier mainLaneTier() {
        if (mainPosition == null) {
            return null;
        }
        return switch (mainPosition) {
            case TOP -> topTier;
            case JUNGLE -> jungleTier;
            case MID -> midTier;
            case ADC -> adcTier;
            case SUPPORT -> supportTier;
        };
    }

    /** 저장된 라인별 티어를 입력칸에 채운다 (아직 없으면 빈칸 그대로). 주 포지션을 먼저 채워 둬야 한다 */
    void fillLaneTiers(LaneTiers laneTiers) {
        if (laneTiers == null) {
            return;
        }
        topTier = laneTiers.top();
        jungleTier = laneTiers.jungle();
        midTier = laneTiers.mid();
        adcTier = laneTiers.adc();
        supportTier = laneTiers.support();
        Tier tier = mainLaneTier();
        Integer lp = mainPosition == null ? null : laneTiers.lpOf(mainPosition);
        if (tier == null || lp == null) {
            return;
        }
        if (tier.isApex()) {
            mainLp = lp;
        } else {
            mainDivision = Tier.divisionOf(lp);
            mainLp = Tier.lpInDivisionOf(lp);
        }
    }

    public static PlayerForm from(Player player) {
        PlayerForm form = new PlayerForm();
        form.setName(player.getName());
        form.setRiotId(player.getRiotId());
        form.setMainPosition(player.getMainPosition());
        form.setSubPosition(player.getSubPosition());
        form.fillLaneTiers(player.getLaneTiers());
        return form;
    }
}
