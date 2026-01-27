package com.msashop.user.config.security;

import com.msashop.user.common.security.GatewayAuthHeaderFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * GatewayAuthHeaderFilter가 서블릿 컨테이너 필터로 "자동 등록"되는 것을 막는다.
 * - 이렇게 해야 SecurityFilterChain(addFilterBefore)로만 실행된다.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<GatewayAuthHeaderFilter> gatewayAuthHeaderFilterRegistration(
            GatewayAuthHeaderFilter filter
    ) {
        FilterRegistrationBean<GatewayAuthHeaderFilter> bean = new FilterRegistrationBean<>(filter);

        // 핵심: 서블릿 필터 체인에 자동 등록되지 않도록 비활성화
        bean.setEnabled(false);

        return bean;
    }
}
