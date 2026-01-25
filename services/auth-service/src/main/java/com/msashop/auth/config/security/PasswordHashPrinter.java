package com.msashop.auth.config.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordHashPrinter {

    @Bean
    CommandLineRunner printPasswordHash(PasswordEncoder passwordEncoder) {
        return args -> {
            String raw = "1234";
            String hash = passwordEncoder.encode(raw);
            System.out.println("[DEV] raw=1234");
            System.out.println("[DEV] argon2Hash=" + hash);
        };
    }
}
