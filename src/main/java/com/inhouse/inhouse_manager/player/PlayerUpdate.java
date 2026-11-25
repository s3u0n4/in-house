package com.inhouse.inhouse_manager.player;

/**
 * 플레이어 한 명의 수정 내용. 목록 수정에서 여러 명을 한 번에 저장할 때 쓴다.
 */
public record PlayerUpdate(Long id, String name, String riotId, Position mainPosition, Position subPosition, Tier tier) {
}
