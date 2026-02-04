package com.msashop.payment.application.port.out;

import com.msashop.payment.domain.model.PaymentTransaction;

public interface SavePaymentPort {
    PaymentTransaction save(PaymentTransaction paymentTransaction);
}

