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

    // 라인별 티어: 본인이 생각하기에 그 라인을 어느 티어 정도로 하는지 (팀 나누기에 쓴다)
    // LP는 마스터 이상일 때만 적을 수 있고, 비워도 된다
    @NotNull(message = "탑 티어를 선택하세요.")
    private Tier topTier;
    @Min(value = 0, message = "LP는 0 이상으로 입력하세요.") @Max(value = MAX_LP, message = "LP는 3000 이하로 입력하세요.")
    private Integer topLp;
    @NotNull(message = "정글 티어를 선택하세요.")
    private Tier jungleTier;
    @Min(value = 0, message = "LP는 0 이상으로 입력하세요.") @Max(value = MAX_LP, message = "LP는 3000 이하로 입력하세요.")
    private Integer jungleLp;
    @NotNull(message = "미드 티어를 선택하세요.")
    private Tier midTier;
    @Min(value = 0, message = "LP는 0 이상으로 입력하세요.") @Max(value = MAX_LP, message = "LP는 3000 이하로 입력하세요.")
    private Integer midLp;
    @NotNull(message = "원딜 티어를 선택하세요.")
    private Tier adcTier;
    @Min(value = 0, message = "LP는 0 이상으로 입력하세요.") @Max(value = MAX_LP, message = "LP는 3000 이하로 입력하세요.")
    private Integer adcLp;
    @NotNull(message = "서폿 티어를 선택하세요.")
    private Tier supportTier;
    @Min(value = 0, message = "LP는 0 이상으로 입력하세요.") @Max(value = MAX_LP, message = "LP는 3000 이하로 입력하세요.")
    private Integer supportLp;

    /** 입력칸 값으로 LaneTiers를 만든다. 마스터 아래 티어에 적힌 LP는 버린다 */
    public LaneTiers toLaneTiers() {
        return new LaneTiers(topTier, apexLp(topTier, topLp), jungleTier, apexLp(jungleTier, jungleLp),
                midTier, apexLp(midTier, midLp), adcTier, apexLp(adcTier, adcLp),
                supportTier, apexLp(supportTier, supportLp));
    }

    private static Integer apexLp(Tier tier, Integer lp) {
        return tier != null && tier.isApex() ? lp : null;
    }

    /** 저장된 라인별 티어를 입력칸에 채운다 (아직 없으면 빈칸 그대로) */
    void fillLaneTiers(LaneTiers laneTiers) {
        if (laneTiers == null) {
            return;
        }
        topTier = laneTiers.top();
        topLp = laneTiers.topLp();
        jungleTier = laneTiers.jungle();
        jungleLp = laneTiers.jungleLp();
        midTier = laneTiers.mid();
        midLp = laneTiers.midLp();
        adcTier = laneTiers.adc();
        adcLp = laneTiers.adcLp();
        supportTier = laneTiers.support();
        supportLp = laneTiers.supportLp();
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
