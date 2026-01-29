package com.msashop.product.adapter.in.web;

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
    // ?몄쬆留??꾩슂 (roles ?놁뼱??userId留??덉쑝硫?"?몄쬆?? 泥섎━?섎?濡??듦낵 媛??
    // => roles源뚯? ?꾩닔濡?媛뺤젣?섍퀬 ?띠쑝硫?GatewayAuthHeaderFilter?먯꽌 rolesHeader ?놁쑝硫?401 泥섎━濡?諛붽씀硫???
    @GetMapping("/me")
    public String myOrders(@AuthenticationPrincipal CurrentUser user) {
        // userId 湲곕컲 議고쉶
        return "my orders for userId=" + user.userId();
    }

    // 愿由ъ옄留?
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/recalculate")
    public String recalc(@AuthenticationPrincipal CurrentUser user) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("auth={}", auth);
        log.debug("authorities={}", auth == null ? null : auth.getAuthorities());

        return "ok by admin userId=" + user.userId();
    }
}

