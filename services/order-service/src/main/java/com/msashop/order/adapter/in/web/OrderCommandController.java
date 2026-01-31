package com.msashop.order.adapter.in.web;

import com.msashop.order.adapter.in.web.dto.CancelOrderRequest;
import com.msashop.order.adapter.in.web.dto.CreateOrderRequest;
import com.msashop.order.adapter.in.web.mapper.OrderWebCommandMapper;
import com.msashop.order.application.port.in.CancelOrderUseCase;
import com.msashop.order.application.port.in.CreateOrderUseCase;
import com.msashop.order.application.port.in.model.CancelOrderCommand;
import com.msashop.order.common.response.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCommandController {

    private final CreateOrderUseCase createOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<Long> create(@AuthenticationPrincipal CurrentUser currentUser,
                                       @Valid @RequestBody CreateOrderRequest request) {
        Long orderId = createOrderUseCase.createOrder(OrderWebCommandMapper.toCommand(currentUser.userId(), request));
        return ResponseEntity.ok(orderId);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal CurrentUser currentUser,
                                       @PathVariable Long orderId,
                                       @Valid @RequestBody CancelOrderRequest request) {
        cancelOrderUseCase.cancelOrder(new CancelOrderCommand(orderId, currentUser.userId(), request.reason()));
        return ResponseEntity.noContent().build();
    }
}
