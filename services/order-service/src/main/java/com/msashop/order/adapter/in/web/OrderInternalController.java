package com.msashop.order.adapter.in.web;

import com.msashop.order.adapter.in.web.dto.MarkPaidRequest;
import com.msashop.order.application.port.in.MarkOrderPaidUseCase;
import com.msashop.order.application.port.in.model.MarkOrderPaidCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final MarkOrderPaidUseCase markOrderPaidUseCase;

    @PostMapping("/{orderId}/mark-paid")
    public ResponseEntity<Void> markPaid(@PathVariable Long orderId,
                                         @Valid @RequestBody MarkPaidRequest request) {
        markOrderPaidUseCase.markPaid(new MarkOrderPaidCommand(orderId, request.paymentId(), request.idempotencyKey(), request.reason()));
        return ResponseEntity.noContent().build();
    }
}

