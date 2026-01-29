package com.msashop.auth.adapter.out.client;

import com.msashop.auth.adapter.out.client.dto.ProvisionUserProfileRequest;
import com.msashop.auth.application.port.out.UserProfileProvisionPort;
import com.msashop.auth.application.port.out.model.UserPofile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * auth -> user-service 프로필 생성 요청 어댑터.
 */
@Component
@RequiredArgsConstructor
public class UserServiceClientAdapter implements UserProfileProvisionPort {

    private final WebClient webClient;

    @Value("${clients.user-service.base-url:http://localhost:8085}")
    private String userServiceBaseUrl;

    @Override
    public void provisionProfile(UserPofile profile) {

        var param = new ProvisionUserProfileRequest(
                profile.authUserId(),
                profile.userName(),
                profile.empNo(),
                profile.pstnName(),
                profile.tel()
        );
        // 내부 인증 헤더는 WebClient 설정이나 Gateway 필터에서 처리한다고 가정
        webClient.post()
                .uri(userServiceBaseUrl + "/internal/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(param)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
