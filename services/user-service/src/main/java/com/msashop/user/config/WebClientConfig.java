package com.msashop.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${security.internal.header-name:X-Internal-Secret}")
    private String internalHeaderName;

    @Value("${security.internal.service-secret:local-internal-secret}")
    private String internalServiceSecret;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader(internalHeaderName, internalServiceSecret)
                .build();
    }
}
