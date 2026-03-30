package com.msashop.order.application.service;

import com.msashop.common.web.exception.BusinessException;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.order.application.event.OrderPaymentSagaEventFactory;
import com.msashop.order.application.port.in.PayOrderUseCase;
import com.msashop.order.application.port.in.model.PayOrderCommand;
import com.msashop.order.application.port.out.LoadOrderPort;
import com.msashop.order.application.port.out.OutboxEventPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PayOrderService implements PayOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;
    private final OutboxEventPort outboxEventPort;
    private final OrderPaymentSagaEventFactory orderPaymentSagaEventFactory;

    @Override
    public void payOrder(PayOrderCommand command) {
        Order order = loadOrderPort.loadOrder(command.orderId());
        if (!order.getUserId().equals(command.userId())) {
            throw new BusinessException(CommonErrorCode.COMMON_UNAUTHORIZED);
        }

        OrderStatus from = order.getStatus();
        try {
            order.startPayment();
        } catch (IllegalStateException e) {
            throw new BusinessException(CommonErrorCode.COMMON_CONFLICT, e.getMessage());
        }

        // 중복 pay 요청이 들어와도 saga가 중복 시작되면 안 된다.
        // 실제 상태가 CREATED -> PENDING_PAYMENT로 바뀐 경우에만
        // 상태 저장, history 저장, outbox 적재를 수행한다.
        if (from != order.getStatus()) {
            saveOrderPort.save(order);
            saveOrderStatusHistoryPort.saveHistory(
                    order.getOrderId(),
                    from,
                    order.getStatus(),
                    "PAYMENT_STARTED",
                    command.userId()
            );

            // 이제 order-service는 payment-service를 직접 호출하지 않는다.
            // 분산 흐름의 첫 단계는 재고 예약이며, 주문 상태 변경과 saga 시작 의도는
            // 같은 로컬 트랜잭션 안에서 함께 커밋돼야 한다.
            outboxEventPort.append(
                    orderPaymentSagaEventFactory.stockReservationRequested(order, command)
            );
        }
    }
}
