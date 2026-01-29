package com.msashop.user.adapter.out.client;

import com.msashop.user.application.port.out.DisableAuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AuthUserClientAdapter implements DisableAuthUserPort {
    private final WebClient webClient;

    @Value("${clients.auth-service.base-url:http://localhost:8081}")
    private String authServiceBaseUrl;

    @Value("${security.internal.header-name:X-Internal-Secret}")
    private String internalHeaderName;

    @Value("${security.internal.service-secret:local-internal-secret}")
    private String internalServiceSecret;

    @Override
    public void disableAuthUser(Long authUserId) {
        webClient.patch()
                .uri(authServiceBaseUrl + "/internal/auth/users/" + authUserId + "/disable")
                .header(internalHeaderName, internalServiceSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
