package com.msashop.payment.application.port.in;

import com.msashop.payment.application.port.in.model.ApprovePaymentCommand;
import com.msashop.payment.application.port.in.model.PaymentResult;

public interface ApprovePaymentUseCase {
    PaymentResult approve(ApprovePaymentCommand command);
}

