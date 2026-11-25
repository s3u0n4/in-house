package com.inhouse.inhouse_manager.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;
import java.util.Set;

/**
 * 화면 테마(lol, opgg)를 쿠키에 저장한다. 화면에 넘기는 일은 ThemeAdvice가 한다.
 */
@Controller
public class ThemeController {
    static final String COOKIE_NAME = "theme";
    static final String DEFAULT_THEME = "lol";
    static final Set<String> THEMES = Set.of("lol", "opgg");

    /** 테마를 바꾸고 보고 있던 화면으로 돌아간다 */
    @GetMapping("/theme/{name}")
    public String change(@PathVariable String name,
                         @RequestHeader(value = "Referer", required = false) String referer,
                         HttpServletResponse response) {
        if (THEMES.contains(name)) {
            Cookie cookie = new Cookie(COOKIE_NAME, name);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 365);
            response.addCookie(cookie);
        }
        return "redirect:" + backPath(referer);
    }

    /** 다른 사이트로 보내지 않도록 Referer에서 경로와 쿼리만 쓴다 */
    private String backPath(String referer) {
        if (referer == null) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || path.isEmpty() || !path.startsWith("/") || path.startsWith("//") || path.startsWith("/theme/")) {
                return "/";
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
