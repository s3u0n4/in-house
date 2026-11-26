package com.inhouse.inhouse_manager.player;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 목록 수정 화면의 폼. 표의 한 줄이 Row 하나다.
 * 화면에서는 rows[0].name, rows[0].topTier ... 처럼 줄 번호를 붙여서 보낸다.
 * 입력값 검사는 삭제하지 않는 줄만 하므로, 여기에 @Valid를 붙이지 않고 PlayerController에서 줄마다 직접 검사한다.
 */
@Getter
@Setter
public class PlayerListForm {
    private List<Row> rows = new ArrayList<>();

    /** PlayerForm의 입력칸과 검증 규칙을 그대로 물려받고, 어느 플레이어인지 알기 위한 id만 더한다 */
    @Getter
    @Setter
    public static class Row extends PlayerForm {
        private Long id;
        /** 체크하면 저장할 때 이 플레이어를 삭제한다 */
        private boolean delete;

        public PlayerUpdate toUpdate() {
            return new PlayerUpdate(id, getName(), getRiotId(), getMainPosition(), getSubPosition(),
                    toLaneTiers());
        }
    }

    public static PlayerListForm from(List<Player> players) {
        PlayerListForm form = new PlayerListForm();
        for (Player player : players) {
            Row row = new Row();
            row.setId(player.getId());
            row.setName(player.getName());
            row.setRiotId(player.getRiotId());
            row.setMainPosition(player.getMainPosition());
            row.setSubPosition(player.getSubPosition());
            row.fillLaneTiers(player.getLaneTiers());
            form.rows.add(row);
        }
        return form;
    }
}
