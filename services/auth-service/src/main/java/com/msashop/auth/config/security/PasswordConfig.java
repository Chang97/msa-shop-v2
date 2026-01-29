package com.msashop.auth.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 湲곕낯 ?뚮씪誘명꽣(媛쒕컻 ?④퀎): ?꾩슂?섎㈃ ?쒕떇
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
