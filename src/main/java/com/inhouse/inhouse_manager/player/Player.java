package com.inhouse.inhouse_manager.player;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String riotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position mainPosition;

    @Enumerated(EnumType.STRING)
    private Position subPosition;

    // 티어 기능 전에 등록한 플레이어는 값이 없어서 DB에서는 null을 허용한다 (새로 등록할 때는 폼에서 필수)
    @Enumerated(EnumType.STRING)
    private Tier tier;

    /**
     * 삭제한 시각. 값이 있으면 목록에서는 빠지고 지난 경기 기록에만 "(삭제됨)"으로 남는다 (소프트 삭제).
     * 관리자가 휴지통에서 되돌릴 수 있다.
     */
    private LocalDateTime deletedAt;

    public Player(String name, String riotId, Position mainPosition, Position subPosition, Tier tier) {
        this.name = name;
        this.riotId = riotId;
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
        this.tier = tier;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 경기 기록 화면에 보여줄 이름. 삭제된 플레이어는 "(삭제됨)"을 붙인다 */
    public String getDisplayName() {
        return isDeleted() ? name + " (삭제됨)" : name;
    }

    /** 경기 기록은 남기고 목록에서만 뺀다. 라이엇 ID는 다른 사람이 다시 쓸 수 있게 바꿔 둔다 */
    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
        this.riotId = deletedRiotIdPrefix() + riotId;
    }

    /** 삭제하기 전의 라이엇 ID (삭제되지 않았으면 지금 라이엇 ID) */
    public String getOriginalRiotId() {
        return isDeleted() && riotId.startsWith(deletedRiotIdPrefix())
                ? riotId.substring(deletedRiotIdPrefix().length()) : riotId;
    }

    /** 되돌린다. 라이엇 ID도 원래대로 돌린다 (같은 ID를 쓰는 사람이 있는지는 PlayerService가 먼저 확인한다) */
    public void restore() {
        this.riotId = getOriginalRiotId();
        this.deletedAt = null;
    }

    private String deletedRiotIdPrefix() {
        return "deleted:" + id + ":";
    }

    public void update(String name, String riotId, Position mainPosition, Position subPosition, Tier tier) {
        this.name = name;
        this.riotId = riotId;
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
        this.tier = tier;
    }
}
