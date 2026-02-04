package com.msashop.payment.adapter.in.web;

import com.msashop.payment.adapter.in.web.dto.ApprovePaymentRequest;
import com.msashop.payment.adapter.in.web.dto.PaymentResponse;
import com.msashop.payment.adapter.in.web.mapper.PaymentWebMapper;
import com.msashop.payment.application.port.in.ApprovePaymentUseCase;
import com.msashop.payment.application.port.in.model.PaymentResult;
import com.msashop.payment.common.response.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final ApprovePaymentUseCase approvePaymentUseCase;

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/approve")
    public ResponseEntity<PaymentResponse> approve(@AuthenticationPrincipal CurrentUser currentUser,
                                                   @Valid @RequestBody ApprovePaymentRequest request) {
        PaymentResult result = approvePaymentUseCase.approve(PaymentWebMapper.toCommand(currentUser, request));
        return ResponseEntity.ok(PaymentWebMapper.toResponse(result));
    }
}
