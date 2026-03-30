package com.msashop.auth.adapter.in.web.cookie;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 리프레시 토큰 쿠키에 적용할 웹 속성을 바인딩하는 설정 객체다.
 *
 * <p>대상 설정 prefix는 {@code app.auth.cookie} 이며,
 * secure, same-site, path 같은 쿠키 옵션을 타입 안전하게 주입받기 위해 사용한다.</p>
 */
@ConfigurationProperties(prefix = "app.auth.cookie")
public record AuthCookieProperties(
        boolean secure,
        String sameSite,
        String path
) {
}
