package com.inhouse.inhouse_manager.match;

import com.inhouse.inhouse_manager.player.Position;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class MatchForm {
    @NotNull(message = "경기 날짜를 입력하세요.")
    @PastOrPresent(message = "미래 날짜는 기록할 수 없습니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate playedAt;

    @NotNull(message = "승리 팀을 선택하세요.")
    private Team winner;

    // 포지션 순서(TOP, JUNGLE, MID, ADC, SUPPORT)대로 플레이어 id를 담는다. 빈 자리는 null
    private List<Long> bluePlayerIds = emptySlots();
    private List<Long> redPlayerIds = emptySlots();

    private static List<Long> emptySlots() {
        return new ArrayList<>(Collections.nCopies(Position.values().length, null));
    }
}
