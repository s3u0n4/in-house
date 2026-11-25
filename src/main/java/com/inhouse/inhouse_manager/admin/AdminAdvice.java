package com.inhouse.inhouse_manager.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에 "관리자로 로그인했는지"를 isAdmin 값으로 넘겨준다. 헤더의 휴지통, 로그아웃 메뉴에 쓴다.
 */
@ControllerAdvice
public class AdminAdvice {

    @ModelAttribute("isAdmin")
    public boolean isAdmin(HttpServletRequest request) {
        return request.isUserInRole("ADMIN");
    }
}
