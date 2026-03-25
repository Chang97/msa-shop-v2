package com.msashop.payment.application.port.out;

import com.msashop.payment.application.port.out.model.PaymentGatewayRequest;
import com.msashop.payment.application.port.out.model.PaymentGatewayResult;

public interface RequestPaymentGatewayPort {
    PaymentGatewayResult request(PaymentGatewayRequest request);
}
