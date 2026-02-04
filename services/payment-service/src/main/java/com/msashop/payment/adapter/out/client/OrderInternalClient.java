package com.msashop.payment.adapter.out.client;

import com.msashop.payment.application.port.out.MarkOrderPaidPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class OrderInternalClient implements MarkOrderPaidPort {

    private final WebClient webClient;

    @Value("${clients.order.base-url}")
    private String orderBaseUrl;

    @Value("${security.internal.header-name:X-Internal-Secret}")
    private String internalHeaderName;

    @Value("${security.internal.service-secret:local-internal-secret}")
    private String internalServiceSecret;

    @Override
    public void markPaid(Long orderId, Long paymentId, String idempotencyKey, String reason) {
        webClient.post()
                .uri(orderBaseUrl + "/internal/orders/" + orderId + "/mark-paid")
                .header(internalHeaderName, internalServiceSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new MarkPaidRequest(paymentId, idempotencyKey, reason))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private record MarkPaidRequest(Long paymentId, String idempotencyKey, String reason) {}
}

