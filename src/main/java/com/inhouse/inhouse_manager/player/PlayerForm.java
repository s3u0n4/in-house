package com.inhouse.inhouse_manager.player;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerForm {
    @NotBlank(message = "이름을 입력하세요.")
    private String name;

    @NotBlank(message = "라이엇 ID를 입력하세요.")
    private String riotId;

    @NotNull(message = "주 포지션을 선택하세요.")
    private Position mainPosition;
    private Position subPosition;
}
