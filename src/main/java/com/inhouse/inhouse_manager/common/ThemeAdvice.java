package com.inhouse.inhouse_manager.common;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * @ControllerAdvice: 모든 컨트롤러에 공통으로 적용된다.
 * 여기 @ModelAttribute 메서드가 돌려준 값은 모든 화면의 Model에 "theme"이라는 이름으로 자동으로 담긴다.
 */
@ControllerAdvice
public class ThemeAdvice {

    @ModelAttribute("theme")
    public String theme(@CookieValue(value = ThemeController.COOKIE_NAME,
            defaultValue = ThemeController.DEFAULT_THEME) String theme) {
        return ThemeController.THEMES.contains(theme) ? theme : ThemeController.DEFAULT_THEME;
    }
}
