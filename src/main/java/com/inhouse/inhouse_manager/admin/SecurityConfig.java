package com.inhouse.inhouse_manager.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 스프링 시큐리티 설정. 일반 사용자 로그인은 없고 관리자 계정 하나만 있다 (2026-11-26 배포 준비).
 * - 누구나: 보기(플레이어, 경기 기록, 랭킹), 팀 나누기, 플레이어 등록 "신청"(POST /players → 관리자 승인 대기)
 * - 관리자만: 그 밖의 모든 저장·수정·삭제(POST), 수정 화면, 경기 기록 화면, /admin/**, H2 콘솔
 * - 관리자 아이디와 비밀번호는 config/application.properties(깃에 올라가지 않는 파일)나 환경 변수에서 읽는다.
 */
@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 관리자 화면과, 기록을 바꾸는 화면(GET)
                        .requestMatchers("/admin/**", "/h2-console/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/players/edit", "/players/*/edit", "/matches/new").hasRole("ADMIN")
                        // 저장·수정·삭제(POST)는 등록 신청만 누구나, 나머지는 모두 관리자만 (새 기능을 만들어도 기본은 막힌다)
                        .requestMatchers(HttpMethod.POST, "/players").permitAll()
                        .requestMatchers(HttpMethod.POST, "/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin/pending")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/players"))
                // H2 콘솔은 자체 화면(frame)과 폼을 써서 CSRF 검사와 frame 차단을 풀어 준다 (관리자만 들어가므로 괜찮다)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService adminAccount(@Value("${inhouse.admin.username:admin}") String username,
                                           @Value("${inhouse.admin.password:}") String password,
                                           @Value("${inhouse.admin.require-password:false}") boolean requirePassword,
                                           PasswordEncoder passwordEncoder) {
        if (password.isBlank() && requirePassword) {
            // 배포 서버(prod)에서는 임시 비밀번호로 열지 않고 바로 멈춘다
            throw new IllegalStateException("관리자 비밀번호가 없습니다. 환경 변수 INHOUSE_ADMIN_PASSWORD를 설정하세요.");
        }
        if (password.isBlank()) {
            // 비밀번호 설정이 없으면 임시 비밀번호를 만들어 로그에 한 번 보여준다 (재시작하면 바뀐다)
            byte[] bytes = new byte[8];
            new SecureRandom().nextBytes(bytes);
            password = HexFormat.of().formatHex(bytes);
            log.warn("관리자 비밀번호 설정(inhouse.admin.password)이 없어서 임시 비밀번호를 만들었습니다: {}", password);
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build());
    }
}
