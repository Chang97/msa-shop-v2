package com.msashop.payment.application.port.out.model;

/**
 * PG 재조회 결과.
 * approved / failed / unknown 중 하나로 해석할 수 있게 단순화한다.
 */
public record PaymentGatewayStatusResult(
        boolean approved,
        boolean failed,
        String providerTxId,
        String reasonCode,
        String reasonMessage
) {
}