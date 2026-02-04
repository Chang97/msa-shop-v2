package com.msashop.payment.adapter.in.web.mapper;

import com.msashop.payment.adapter.in.web.dto.ApprovePaymentRequest;
import com.msashop.payment.adapter.in.web.dto.PaymentResponse;
import com.msashop.payment.application.port.in.model.ApprovePaymentCommand;
import com.msashop.payment.application.port.in.model.PaymentResult;
import com.msashop.payment.common.response.CurrentUser;

public final class PaymentWebMapper {
    private PaymentWebMapper() {}

    public static ApprovePaymentCommand toCommand(CurrentUser currentUser, ApprovePaymentRequest request) {
        return new ApprovePaymentCommand(request.orderId(), request.amount(), request.idempotencyKey(), currentUser);
    }

    public static PaymentResponse toResponse(PaymentResult result) {
        return new PaymentResponse(result.paymentId(), result.orderId(), result.status());
    }
}

