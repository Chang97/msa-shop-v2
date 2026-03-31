package com.msashop.order.application.service;

import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingPaymentExpirationServiceTest {

    @Mock
    private LoadOrderPort loadOrderPort;

    @Mock
    private SaveOrderPort saveOrderPort;

    @Mock
    private SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;

    @InjectMocks
    private PendingPaymentExpirationService service;

    @Test
    @DisplayName("PENDING_PAYMENT 주문은 만료 시 PAYMENT_EXPIRED로 전이한다")
    void should_expire_pending_payment_order() {
        Instant threshold = Instant.parse("2026-03-31T00:00:00Z");
        Order order = OrderServiceFixtures.order(OrderStatus.PENDING_PAYMENT);
        when(loadOrderPort.loadPendingPaymentOrderIdsBefore(threshold, 50)).thenReturn(List.of(1L));
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        service.expirePendingPayments(threshold, 50);

        verify(saveOrderPort).save(order);
        verify(saveOrderStatusHistoryPort).saveHistory(
                1L,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAYMENT_EXPIRED,
                "PAYMENT_EXPIRED",
                1L
        );
    }

    @Test
    @DisplayName("만료 대상 주문이 없으면 아무 작업도 하지 않는다")
    void should_do_nothing_when_no_pending_payment_order_exists() {
        Instant threshold = Instant.parse("2026-03-31T00:00:00Z");
        when(loadOrderPort.loadPendingPaymentOrderIdsBefore(threshold, 50)).thenReturn(List.of());

        service.expirePendingPayments(threshold, 50);

        verify(saveOrderPort, never()).save(any());
        verify(saveOrderStatusHistoryPort, never()).saveHistory(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 PAYMENT_EXPIRED인 주문은 추가 저장이나 이력을 남기지 않는다")
    void should_not_save_when_order_is_already_expired() {
        Instant threshold = Instant.parse("2026-03-31T00:00:00Z");
        Order order = OrderServiceFixtures.order(OrderStatus.PAYMENT_EXPIRED);
        when(loadOrderPort.loadPendingPaymentOrderIdsBefore(threshold, 50)).thenReturn(List.of(1L));
        when(loadOrderPort.loadOrder(1L)).thenReturn(order);

        service.expirePendingPayments(threshold, 50);

        verify(saveOrderPort, never()).save(any());
        verify(saveOrderStatusHistoryPort, never()).saveHistory(any(), any(), any(), any(), any());
    }
}
