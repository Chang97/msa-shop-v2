package com.msashop.order.application.service;

import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PendingPaymentExpirationService {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

    @Transactional
    public void expirePendingPayments(Instant threshold, int batchSize) {
        for (Long orderId : loadOrderPort.loadPendingPaymentOrderIdsBefore(threshold, batchSize)) {
            expirePendingPayment(orderId);
        }
    }

    private void expirePendingPayment(Long orderId) {
        Order order = loadOrderPort.loadOrder(orderId);
        OrderStatus from = order.getStatus();

        order.markPaymentExpired();

        if (from != order.getStatus()) {
            saveOrderPort.save(order);
            saveOrderStatusHistoryPort.saveHistory(
                    order.getOrderId(),
                    from,
                    order.getStatus(),
                    "PAYMENT_EXPIRED",
                    order.getUserId()
            );
        }
    }
}
