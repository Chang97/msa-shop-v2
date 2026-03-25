package com.msashop.payment.application.port.out;

import com.msashop.payment.application.port.out.model.PaymentGatewayStatusResult;

/**
 * PG 승인 여부를 사후 재조회하기 위한 포트.
 * APPROVAL_UNKNOWN 상태를 reconciliation 할 때 사용한다.
 */
public interface GetPaymentStatusGatewayPort {
    PaymentGatewayStatusResult getStatus(String provider, String idempotencyKey, String providerTxId);
}