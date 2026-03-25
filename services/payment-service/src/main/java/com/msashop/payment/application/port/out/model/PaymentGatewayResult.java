package com.msashop.payment.application.port.out.model;

public record PaymentGatewayResult(
        boolean approved,
        String provider,
        String providerTxId,
        String reasonCode,
        String reasonMessage
) {
}