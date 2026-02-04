package com.msashop.payment.application.port.out;

import com.msashop.payment.common.response.CurrentUser;

public interface RequestOrderPaymentPort {
    void startPayment(Long orderId, CurrentUser currentUser);
}

