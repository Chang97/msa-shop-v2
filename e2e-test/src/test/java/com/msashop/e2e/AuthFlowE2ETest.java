package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 로그인 API 기본 동작 E2E 점검.
 * - 리프레시/로그아웃 흐름은 아직 포함하지 않음.
 */
class AuthFlowE2ETest {

    private final E2EClient client = new E2EClient();

    @Test
    void login_should_return_access_token() {
        // given: 로그인 자격 증명
        Response response = client.postJson(
                "/api/auth/login",
                null,
                TestFixtures.userLogin()
        );

        // then: 200 응답에 accessToken 존재 확인
        assertEquals(200, response.statusCode());
        String accessToken = E2EExtractors.accessToken(response);
        assertNotNull(accessToken);
        assertFalse(accessToken.isBlank());
    }

    @Test
    void refresh_rotates_and_logout_invalidates() {
        // 1) 로그인 : accessToken/rt 확보
        Response login = client.postJson(
                "/api/auth/login",
                null,
                TestFixtures.userLogin()
        );
        assertEquals(200, login.statusCode());
        String accessToken1 = E2EExtractors.accessToken(login);
        String rt1 = login.getCookie("rt");
        assumeTrue(rt1 != null && !rt1.isBlank(), "rt 쿠키가 없습니다.");

        // 2) rt로 1회 리프레시 -> 회전된 rt2/accessToken2 확보
        Response refresh1 = client.postWithCookie(
                "/api/auth/refresh",
                "rt",
                rt1
        );
        assertEquals(200, refresh1.statusCode());
        String accessToken2 = E2EExtractors.accessToken(refresh1);
        String rt2 = refresh1.getCookie("rt");
        assertNotNull(rt2);

        // 3) 최신 accessToken2로 로그아웃 -> 서버가 rt2/세션을 무효화한다는 가정
        Response logout = client.postWithCookie(
                "/api/auth/logout",
                "rt",
                rt2
        );
        assertEquals(204, logout.statusCode());

        // 4) 로그아웃 이후 같은 rt2로 리프레시 시도 -> 실패 기대
        Response refreshAfterLogout = client.postWithCookie(
                "/api/auth/refresh",
                "rt",
                rt2
        );
        assertTrue(refreshAfterLogout.statusCode() == 401 || refreshAfterLogout.statusCode() == 403,
                "로그아웃 후 리프레시는 실패해야 합니다. actual=" + refreshAfterLogout.statusCode());
    }
}
