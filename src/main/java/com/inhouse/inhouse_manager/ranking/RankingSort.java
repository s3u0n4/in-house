package com.inhouse.inhouse_manager.ranking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 랭킹 정렬 기준. 화면 위쪽 탭이 이 값 하나씩이다 (?sort=RATE) */
@Getter
@RequiredArgsConstructor
public enum RankingSort {
    RATE("승률", "보정 승률 (승+1)/(경기+2) 순. 경기 수가 적으면 50% 쪽으로 당겨져서, 1전 1승이 4전 4승보다 위로 가지 않아요."),
    WINS("다승", "이긴 경기 수 순. 같으면 보정 승률이 높은 쪽이 위예요."),
    POWER("전력", "팀 나누기에 쓰는 전력 순. 주 포지션 라인 티어 점수 + 내전 성적 보정(최대 ±2)이에요.");

    private final String label;
    private final String description;
}
