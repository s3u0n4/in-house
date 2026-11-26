package com.inhouse.inhouse_manager.player;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Stream;

@Getter
@Setter
public class PlayerForm {
    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 30, message = "이름은 30자 이하로 입력하세요.")
    private String name;

    @NotBlank(message = "라이엇 ID를 입력하세요.")
    @Size(max = 50, message = "라이엇 ID는 50자 이하로 입력하세요.")
    private String riotId;

    @NotNull(message = "주 포지션을 선택하세요.")
    private Position mainPosition;
    private Position subPosition;

    @NotNull(message = "티어를 선택하세요.")
    private Tier tier;

    // 라인별 티어: 본인이 생각하기에 그 라인을 어느 티어 정도로 하는지 (팀 나누기에 쓴다)
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

    /** 라인별 티어에는 언랭을 고를 수 없다 (화면 선택지에도 없지만, 주소를 직접 조작한 요청을 막는다) */
    @AssertTrue(message = "라인별 티어에는 언랭을 고를 수 없어요.")
    public boolean isLaneTiersRanked() {
        return Stream.of(topTier, jungleTier, midTier, adcTier, supportTier)
                .noneMatch(t -> t == Tier.UNRANKED);
    }

    public LaneTiers toLaneTiers() {
        return new LaneTiers(topTier, jungleTier, midTier, adcTier, supportTier);
    }

    /** 저장된 라인별 티어를 입력칸 5개에 채운다 (아직 없으면 빈칸 그대로) */
    void fillLaneTiers(LaneTiers laneTiers) {
        if (laneTiers == null) {
            return;
        }
        topTier = laneTiers.top();
        jungleTier = laneTiers.jungle();
        midTier = laneTiers.mid();
        adcTier = laneTiers.adc();
        supportTier = laneTiers.support();
    }

    public static PlayerForm from(Player player) {
        PlayerForm form = new PlayerForm();
        form.setName(player.getName());
        form.setRiotId(player.getRiotId());
        form.setMainPosition(player.getMainPosition());
        form.setSubPosition(player.getSubPosition());
        form.setTier(player.getTier());
        form.fillLaneTiers(player.getLaneTiers());
        return form;
    }
}
