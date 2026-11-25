package com.inhouse.inhouse_manager.match;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Team {
    BLUE("블루"), RED("레드");

    private final String label;
}
