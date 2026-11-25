package com.inhouse.inhouse_manager.player;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

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

    public static PlayerForm from(Player player) {
        PlayerForm form = new PlayerForm();
        form.setName(player.getName());
        form.setRiotId(player.getRiotId());
        form.setMainPosition(player.getMainPosition());
        form.setSubPosition(player.getSubPosition());
        return form;
    }
}
