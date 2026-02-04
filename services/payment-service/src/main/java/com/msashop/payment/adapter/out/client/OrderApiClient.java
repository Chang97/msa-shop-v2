package com.msashop.payment.adapter.out.client;

import com.msashop.payment.application.port.out.RequestOrderPaymentPort;
import com.msashop.payment.common.response.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OrderApiClient implements RequestOrderPaymentPort {

    private final WebClient webClient;

    @Value("${clients.order.base-url}")
    private String orderBaseUrl;

    @Override
    public void startPayment(Long orderId, CurrentUser currentUser) {
        String rolesHeader = String.join(",", Optional.ofNullable(currentUser.roles()).orElse(Set.of()));

        webClient.post()
                .uri(orderBaseUrl + "/api/orders/" + orderId + "/pay")
                .header("X-User-Id", String.valueOf(currentUser.userId()))
                .header("X-Roles", rolesHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}

