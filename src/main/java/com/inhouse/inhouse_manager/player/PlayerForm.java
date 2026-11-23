package com.inhouse.inhouse_manager.player;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerForm {
    private String name;
    private String riotId;
    private Position mainPosition;
    private Position subPosition;
}
