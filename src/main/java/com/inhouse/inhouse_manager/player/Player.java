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
     * 경기 기록이 있는 플레이어를 삭제한 시각. 값이 있으면 목록에서는 빠지고 지난 경기 기록에만 남는다 (소프트 삭제).
     * 경기 기록이 없는 플레이어는 DB에서 바로 지우므로 이 값을 쓰지 않는다.
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
        this.riotId = "deleted:" + id + ":" + riotId;
    }

    public void update(String name, String riotId, Position mainPosition, Position subPosition, Tier tier) {
        this.name = name;
        this.riotId = riotId;
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
        this.tier = tier;
    }
}
