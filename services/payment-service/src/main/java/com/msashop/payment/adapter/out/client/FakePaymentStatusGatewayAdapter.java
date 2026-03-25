package com.msashop.payment.adapter.out.client;

import com.msashop.payment.application.port.out.GetPaymentStatusGatewayPort;
import com.msashop.payment.application.port.out.model.PaymentGatewayStatusResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * reconciliation 단계에서 사용할 가짜 PG 상태 조회 adapter.
 *
 * 정책:
 * - idempotencyKey가 UNKNOWN-FAIL- 로 시작하면 실패 확정
 * - 그 외는 승인 확정
 */
@Component
public class FakePaymentStatusGatewayAdapter implements GetPaymentStatusGatewayPort {

    @Override
    public PaymentGatewayStatusResult getStatus(String provider, String idempotencyKey, String providerTxId) {
        if (idempotencyKey != null && idempotencyKey.startsWith("UNKNOWN-FAIL-")) {
            return new PaymentGatewayStatusResult(
                    false,
                    true,
                    providerTxId,
                    "PG_RECON_FAILED",
                    "가짜 PG 재조회 실패 시나리오"
            );
        }

        return new PaymentGatewayStatusResult(
                true,
                false,
                providerTxId != null ? providerTxId : "recon-" + UUID.randomUUID(),
                null,
                null
        );
    }
}