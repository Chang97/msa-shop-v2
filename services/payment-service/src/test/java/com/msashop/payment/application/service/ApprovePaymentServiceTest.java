package com.msashop.payment.application.service;

import com.msashop.payment.adapter.out.persistence.repo.PaymentQueryJpaRepository;
import com.msashop.payment.application.port.in.ApprovePaymentUseCase;
import com.msashop.payment.application.port.in.model.ApprovePaymentCommand;
import com.msashop.payment.application.port.in.model.PaymentResult;
import com.msashop.payment.application.port.out.MarkOrderPaidPort;
import com.msashop.payment.application.port.out.RequestOrderPaymentPort;
import com.msashop.payment.common.response.CurrentUser;
import com.msashop.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ApprovePaymentServiceTest {

    @Autowired
    private ApprovePaymentUseCase approvePaymentUseCase;

    @Autowired
    private PaymentQueryJpaRepository paymentQueryJpaRepository;

    @MockBean
    private RequestOrderPaymentPort requestOrderPaymentPort;

    @MockBean
    private MarkOrderPaidPort markOrderPaidPort;

    @Test
    void approvesIdempotentlyWithSameKey() {
        String idempotencyKey = "PAY-1-" + UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(1L, Set.of("ROLE_USER"));
        ApprovePaymentCommand command = new ApprovePaymentCommand(1L, BigDecimal.valueOf(1000), idempotencyKey, currentUser);

        PaymentResult first = approvePaymentUseCase.approve(command);
        PaymentResult second = approvePaymentUseCase.approve(command);

        assertThat(first.paymentId()).isEqualTo(second.paymentId());
        assertThat(second.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(paymentQueryJpaRepository.count()).isEqualTo(1);
        verify(requestOrderPaymentPort, times(1)).startPayment(eq(1L), any());
        verify(markOrderPaidPort, times(2)).markPaid(eq(1L), anyLong(), eq(idempotencyKey), anyString());
    }
}
