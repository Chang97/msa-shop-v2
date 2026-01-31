package com.msashop.order.adapter.in.web;

import com.msashop.order.adapter.in.web.dto.OrderResponse;
import com.msashop.order.adapter.in.web.mapper.OrderWebQueryMapper;
import com.msashop.order.application.port.in.GetOrderUseCase;
import com.msashop.order.application.port.in.GetOrdersUseCase;
import com.msashop.order.common.response.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final GetOrderUseCase getOrderUseCase;
    private final GetOrdersUseCase getOrdersUseCase;

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> myOrders(@AuthenticationPrincipal CurrentUser currentUser) {
        List<OrderResponse> responses = getOrdersUseCase.getOrdersByUser(currentUser.userId()).stream()
                .map(OrderWebQueryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@AuthenticationPrincipal CurrentUser currentUser,
                                                  @PathVariable Long orderId) {
        // Optionally check ownership in service layer
        return ResponseEntity.ok(OrderWebQueryMapper.toResponse(getOrderUseCase.getOrder(orderId)));
    }
}
