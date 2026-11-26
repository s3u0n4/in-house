package com.inhouse.inhouse_manager.admin;

import com.inhouse.inhouse_manager.player.PlayerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에 "관리자로 로그인했는지"를 isAdmin 값으로 넘겨준다.
 * 헤더의 관리자 메뉴와, 수정·삭제·경기 기록 버튼을 관리자에게만 보여줄 때 쓴다.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class AdminAdvice {
    private final PlayerService playerService;

    @ModelAttribute("isAdmin")
    public boolean isAdmin(HttpServletRequest request) {
        return request.isUserInRole("ADMIN");
    }

    /** 승인 대기 중인 등록 신청 수 (헤더의 "승인 대기 n"). 관리자가 아니면 계산하지 않는다 */
    @ModelAttribute("pendingCount")
    public long pendingCount(HttpServletRequest request) {
        return request.isUserInRole("ADMIN") ? playerService.countPending() : 0;
    }
}
