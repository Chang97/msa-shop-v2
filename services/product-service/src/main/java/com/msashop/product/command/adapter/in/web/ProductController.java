package com.msashop.product.command.adapter.in.web;

import com.msashop.product.common.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    // 인증만 필요 (roles 없어도 userId만 있으면 "인증됨" 처리되므로 통과 가능)
    // => roles까지 필수로 강제하고 싶으면 GatewayAuthHeaderFilter에서 rolesHeader 없으면 401 처리로 바꾸면 됨
    @GetMapping("/me")
    public String myOrders(@AuthenticationPrincipal CurrentUser user) {
        // userId 기반 조회
        return "my orders for userId=" + user.userId();
    }

    // 관리자만
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/recalculate")
    public String recalc(@AuthenticationPrincipal CurrentUser user) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("auth={}", auth);
        log.debug("authorities={}", auth == null ? null : auth.getAuthorities());

        return "ok by admin userId=" + user.userId();
    }
}
