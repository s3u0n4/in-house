package com.inhouse.inhouse_manager.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * 스프링 시큐리티 설정.
 * - 관리자 화면(/admin/**)과 H2 콘솔은 관리자로 로그인해야 들어갈 수 있다.
 * - 나머지 화면은 지금처럼 누구나 쓸 수 있다.
 * - 관리자 계정은 하나이고, 아이디와 비밀번호는 config/application.properties(깃에 올라가지 않는 파일)에서 읽는다.
 */
@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**", "/h2-console/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin/trash")
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
                                           PasswordEncoder passwordEncoder) {
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
