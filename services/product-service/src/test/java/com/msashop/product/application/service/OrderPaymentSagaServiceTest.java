package com.msashop.product.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.common.event.EventEnvelope;
import com.msashop.common.event.EventTypes;
import com.msashop.common.event.payload.PaymentFailedPayload;
import com.msashop.common.event.payload.StockReservationItemPayload;
import com.msashop.common.event.payload.StockReservationRequestedPayload;
import com.msashop.product.application.event.ProductSagaEventFactory;
import com.msashop.product.application.port.out.OutboxEventPort;
import com.msashop.product.application.port.out.ProcessedEventPort;
import com.msashop.product.application.port.out.StockReservationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderPaymentSagaService 단위 테스트.
 *
 * 검증 목적:
 * - product-service가 예약재고의 진실 소스로서 올바르게 동작하는지
 * - 예약 성공 시 다음 saga 단계로 이벤트를 넘기는지
 * - 결제 실패 시 예약을 해제하고 재고 복구 책임을 수행하는지
 */
@ExtendWith(MockitoExtension.class)
class OrderPaymentSagaServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private StockReservationPort stockReservationPort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private ProductSagaEventFactory productSagaEventFactory;

    @InjectMocks
    private OrderPaymentSagaService service;

    /**
     * 재고 예약 성공 케이스.
     *
     * 시나리오:
     * - StockReservationRequested 이벤트 수신
     * - 기존 활성 reservation 없음
     * - reserve 호출이 정상 수행됨
     *
     * 기대값:
     * - 새 reservationId를 발급해 reserve를 호출한다
     * - StockReserved 이벤트를 outbox에 적재한다
     * - 원본 이벤트는 processed 처리된다
     */
    @Test
    void should_reserve_stock_and_append_stock_reserved_event() throws Exception {
        EventEnvelope sourceEvent = stockReservationRequestedEvent();
        StockReservationRequestedPayload payload = stockReservationRequestedPayload();

        EventEnvelope reservedEvent = new EventEnvelope(
                "event-2",
                EventTypes.STOCK_RESERVED,
                "STOCK_RESERVATION",
                "reservation-1",
                "saga-1",
                "corr-1",
                "event-1",
                "product-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );

        when(processedEventPort.claim(
                eq("product-group"),
                eq(sourceEvent),
                eq("product-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(sourceEvent.payloadJson(), StockReservationRequestedPayload.class)).thenReturn(payload);
        when(stockReservationPort.findActiveReservationId(1L)).thenReturn(Optional.empty());
        when(productSagaEventFactory.stockReserved(eq(sourceEvent), eq(payload), anyString())).thenReturn(reservedEvent);

        boolean handled = service.handle("product-group", "product-worker", 300L, sourceEvent);

        assertTrue(handled);
        verify(stockReservationPort).reserve(anyString(), eq(1L), eq(payload.items()));
        verify(outboxEventPort).append(reservedEvent);
        verify(processedEventPort).markProcessed(eq("product-group"), eq("event-1"), any(Instant.class));
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }

    /**
     * 결제 실패 후 예약 해제 케이스.
     *
     * 시나리오:
     * - PaymentFailed 이벤트 수신
     *
     * 기대값:
     * - reservationId 기준으로 예약 해제가 호출된다
     * - 이 과정에서 product-service는 예약 재고를 다시 판매 가능 재고로 복구한다
     * - 추가 saga 이벤트는 발행하지 않는다
     * - 원본 이벤트는 processed 처리된다
     */
    @Test
    void should_release_reservation_when_payment_failed_event_is_received() throws Exception {
        EventEnvelope failureEvent = paymentFailedEvent();
        PaymentFailedPayload payload = new PaymentFailedPayload(
                1L,
                "reservation-1",
                "idem-1",
                "FAKE",
                "PG_FAILED",
                "forced failure"
        );

        when(processedEventPort.claim(
                eq("product-group"),
                eq(failureEvent),
                eq("product-worker"),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(true);
        when(objectMapper.readValue(failureEvent.payloadJson(), PaymentFailedPayload.class)).thenReturn(payload);

        boolean handled = service.handle("product-group", "product-worker", 300L, failureEvent);

        assertTrue(handled);
        verify(stockReservationPort).release("reservation-1");
        verify(processedEventPort).markProcessed(eq("product-group"), eq("event-3"), any(Instant.class));
        verify(outboxEventPort, never()).append(any());
        verify(processedEventPort, never()).releaseClaim(any(), any(), any());
    }

    private EventEnvelope stockReservationRequestedEvent() {
        return new EventEnvelope(
                "event-1",
                EventTypes.STOCK_RESERVATION_REQUESTED,
                "ORDER",
                "1",
                "saga-1",
                "corr-1",
                null,
                "order-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private EventEnvelope paymentFailedEvent() {
        return new EventEnvelope(
                "event-3",
                EventTypes.PAYMENT_FAILED,
                "PAYMENT",
                "1",
                "saga-1",
                "corr-1",
                "event-2",
                "payment-service",
                "order.payment.saga.v1",
                "1",
                Instant.now(),
                "{\"orderId\":1}"
        );
    }

    private StockReservationRequestedPayload stockReservationRequestedPayload() {
        return new StockReservationRequestedPayload(
                1L,
                1L,
                new BigDecimal("10000"),
                "KRW",
                "idem-1",
                "FAKE",
                List.of(new StockReservationItemPayload(10L, 2))
        );
    }
}