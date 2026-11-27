package com.inhouse.inhouse_manager.player;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.StringJoiner;

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

    /**
     * 라인별 티어 (팀 나누기용). 이 기능 전에 등록한 플레이어는 null.
     * (예전에 있던 솔랭 티어는 2026-11-26에 뺐다. DB의 tier 컬럼은 남아 있지만 쓰지 않는다)
     */
    @Embedded
    private LaneTiers laneTiers;

    /**
     * 삭제한 시각. 값이 있으면 목록에서는 빠지고 지난 경기 기록에만 "(삭제됨)"으로 남는다 (소프트 삭제).
     * 관리자가 휴지통에서 되돌릴 수 있다.
     */
    private LocalDateTime deletedAt;

    /**
     * 승인 대기. 누구나 등록 신청을 할 수 있고, 관리자가 승인하기 전까지는 목록·팀 나누기·경기 기록·랭킹에 나오지 않는다.
     * 관리자가 직접 등록하면 처음부터 false. (DB에 컬럼을 새로 만들 때 기존 플레이어는 default false = 승인된 것으로 들어간다)
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean pending;

    public Player(String name, String riotId, Position mainPosition, Position subPosition, LaneTiers laneTiers) {
        this.name = name;
        this.riotId = riotId;
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
        this.laneTiers = laneTiers;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 목록, 팀 나누기, 새 경기 기록에 쓸 수 있는 플레이어 (삭제되지 않았고 승인된 사람) */
    public boolean isActive() {
        return !isDeleted() && !pending;
    }

    /** 등록 신청으로 만든다 (승인 대기) */
    static Player requested(String name, String riotId, Position mainPosition, Position subPosition, LaneTiers laneTiers) {
        Player player = new Player(name, riotId, mainPosition, subPosition, laneTiers);
        player.pending = true;
        return player;
    }

    /** 관리자가 등록 신청을 승인한다 */
    public void approve() {
        this.pending = false;
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

    public void update(String name, String riotId, Position mainPosition, Position subPosition, LaneTiers laneTiers) {
        this.name = name;
        this.riotId = riotId;
        this.mainPosition = mainPosition;
        this.subPosition = subPosition;
        this.laneTiers = laneTiers;
    }

    /**
     * 라인별 티어. 주 라인의 세부 LP만 남긴다 (2026-11-28부터 주 라인만 세부 점수를 쓴다.
     * 그 전에 마스터 이상 부 라인에 적은 LP가 DB에 남아 있어도 화면과 팀 나누기에는 쓰지 않는다)
     */
    public LaneTiers getLaneTiers() {
        return laneTiers == null ? null : laneTiers.keepLpOnly(mainPosition);
    }

    /** 라인별 티어를 다 넣었는지. 안 넣었으면 팀 나누기는 그날 참가자 평균으로 계산한다 */
    public boolean hasLaneTiers() {
        return laneTiers != null && laneTiers.isComplete();
    }

    /** 주 포지션의 라인 티어 (목록 화면의 티어 배지 색). 라인별 티어가 없으면 null */
    public Tier getMainLaneTier() {
        return hasLaneTiers() ? laneTiers.of(mainPosition) : null;
    }

    /** 다섯 라인의 티어를 한 줄로. 예: "탑 골드 · 정글 실버 · 미드 마스터 350LP · 원딜 골드 · 서폿 브론즈". 없으면 "-" */
    public String getLaneSummary() {
        if (!hasLaneTiers()) {
            return "-";
        }
        String[] names = {"탑", "정글", "미드", "원딜", "서폿"};
        StringJoiner joiner = new StringJoiner(" · ");
        for (Position position : Position.values()) {
            joiner.add(names[position.ordinal()] + " " + getLaneTiers().labelOf(position));
        }
        return joiner.toString();
    }

    /** 주 포지션의 라인 티어 이름. 예: "다이아몬드", "골드 3 50LP", "마스터 350LP". 없으면 "-" */
    public String getMainLaneLabel() {
        return hasLaneTiers() ? laneTiers.labelOf(mainPosition) : "-";
    }
}
