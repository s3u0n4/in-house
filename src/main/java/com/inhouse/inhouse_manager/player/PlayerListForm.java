package com.inhouse.inhouse_manager.player;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 목록 수정 화면의 폼. 표의 한 줄이 Row 하나다.
 * 화면에서는 rows[0].name, rows[0].tier ... 처럼 줄 번호를 붙여서 보낸다.
 */
@Getter
@Setter
public class PlayerListForm {
    // @Valid: 리스트 안의 Row마다 PlayerForm의 검증 규칙(@NotBlank 등)을 적용한다
    @Valid
    private List<Row> rows = new ArrayList<>();

    /** PlayerForm의 입력칸과 검증 규칙을 그대로 물려받고, 어느 플레이어인지 알기 위한 id만 더한다 */
    @Getter
    @Setter
    public static class Row extends PlayerForm {
        private Long id;

        public PlayerUpdate toUpdate() {
            return new PlayerUpdate(id, getName(), getRiotId(), getMainPosition(), getSubPosition(), getTier());
        }
    }

    public static PlayerListForm from(List<Player> players) {
        PlayerListForm form = new PlayerListForm();
        for (Player player : players) {
            Row row = new Row();
            row.setId(player.getId());
            row.setName(player.getName());
            row.setRiotId(player.getRiotId());
            row.setTier(player.getTier());
            row.setMainPosition(player.getMainPosition());
            row.setSubPosition(player.getSubPosition());
            form.rows.add(row);
        }
        return form;
    }
}
