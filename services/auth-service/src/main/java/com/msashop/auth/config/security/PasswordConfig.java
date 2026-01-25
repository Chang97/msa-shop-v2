package com.msashop.auth.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 기본 파라미터(개발 단계): 필요하면 튜닝
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
