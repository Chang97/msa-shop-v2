package com.msashop.payment.adapter.out.client;

import com.msashop.payment.application.port.out.RequestPaymentGatewayPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayRequest;
import com.msashop.payment.application.port.out.model.PaymentGatewayResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 실제 PG가 없는 환경에서 saga 흐름을 검증하기 위한 fake adapter.
 *
 * 규칙:
 * - idempotencyKey가 FAIL- 로 시작하면 실패 반환
 * - idempotencyKey가 UNKNOWN- 로 시작하면 timeout 과 같은 예외 발생
 * - 그 외에는 승인 성공 반환
 */
@Component
public class FakePaymentGatewayAdapter implements RequestPaymentGatewayPort {

    @Override
    public PaymentGatewayResult request(PaymentGatewayRequest request) {
        if (request.idempotencyKey() != null && request.idempotencyKey().startsWith("UNKNOWN-")) {
            throw new IllegalStateException("fake pg timeout");
        }

        if (request.idempotencyKey() != null && request.idempotencyKey().startsWith("FAIL-")) {
            return new PaymentGatewayResult(
                    false,
                    request.provider(),
                    null,
                    "PG_APPROVAL_FAILED",
                    "가짜 PG 실패 시나리오"
            );
        }

        return new PaymentGatewayResult(
                true,
                request.provider(),
                UUID.randomUUID().toString(),
                null,
                null
        );
    }
}
