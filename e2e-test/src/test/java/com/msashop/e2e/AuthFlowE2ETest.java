package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// 로그인, 토큰 재발급, 로그아웃의 기본 인증 흐름을 검증한다.
class AuthFlowE2ETest {

    private final E2EClient client = new E2EClient();

    // 로그인 성공 시 access token 이 발급되는지 검증한다.
    @Test
    void login_should_return_access_token() {
        Response response = client.postJson(
                "/api/auth/login",
                null,
                TestFixtures.userLogin()
        );

        assertEquals(200, response.statusCode());
        String accessToken = E2EExtractors.accessToken(response);
        assertNotNull(accessToken);
        assertFalse(accessToken.isBlank());
    }

    // refresh token 이 회전되고 로그아웃 후에는 재사용할 수 없는지 검증한다.
    @Test
    void refresh_rotates_and_logout_invalidates() {
        // 1) 로그인으로 access token 과 refresh token 을 획득한다.
        Response login = client.postJson(
                "/api/auth/login",
                null,
                TestFixtures.userLogin()
        );
        assertEquals(200, login.statusCode());

        String rt1 = login.getCookie("rt");
        assumeTrue(rt1 != null && !rt1.isBlank(), "rt 쿠키가 없습니다.");

        // 2) refresh 호출로 새 refresh token 과 access token 을 발급받는다.
        Response refresh1 = client.postWithCookie(
                "/api/auth/refresh",
                "rt",
                rt1
        );
        assertEquals(200, refresh1.statusCode());

        String accessToken2 = E2EExtractors.accessToken(refresh1);
        String rt2 = refresh1.getCookie("rt");
        assertNotNull(accessToken2);
        assertFalse(accessToken2.isBlank());
        assertNotNull(rt2);
        assertNotEquals(rt1, rt2);

        // 3) 최신 refresh token 으로 로그아웃한다.
        Response logout = client.postWithCookie(
                "/api/auth/logout",
                "rt",
                rt2
        );
        assertEquals(204, logout.statusCode());

        // 4) 로그아웃 이후 같은 refresh token 으로 다시 refresh 하면 실패해야 한다.
        Response refreshAfterLogout = client.postWithCookie(
                "/api/auth/refresh",
                "rt",
                rt2
        );
        assertTrue(refreshAfterLogout.statusCode() == 401 || refreshAfterLogout.statusCode() == 403,
                "로그아웃 후 refresh 는 실패해야 합니다. actual=" + refreshAfterLogout.statusCode());
    }
}
