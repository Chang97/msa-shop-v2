package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.PollingSupport;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 회원가입 choreography saga의 사용자 관점 E2E.
 * 포인트는 "register 응답 직후"가 아니라 "사가 완료 후 최종 상태"를 검증하는 것이다.
 */
public class AuthRegisterSagaE2ETest {
    private final E2EClient client = new E2EClient();

    @Test
    void register_success_should_eventually_enable_login() {
        String suffix = String.valueOf(System.currentTimeMillis());
        TestFixtures.RegisterRequest request = TestFixtures.register(suffix);

        // 1. 회원가입 API는 auth DB 반영 + outbox 적재까지 끝나면 즉시 200을 반환한다.
        Response register = client.register(request);
        assertEquals(200, register.statusCode());

        // 2. saga 완료 전에는 login이 실패할 수 있으므로 polling으로 최종 성공을 기다린다.
        Response login = PollingSupport.pollUntil(
                Duration.ofSeconds(15),
                Duration.ofMillis(500),
                () -> client.login(request.loginId(), request.password()),
                response -> response.statusCode() == 200
        );

        String accessToken = E2EExtractors.accessToken(login);
        assertNotNull(accessToken);
        assertFalse(accessToken.isBlank());
    }

    @Test
    void register_success_should_eventually_create_profile() {
        String suffix = String.valueOf(System.nanoTime());
        TestFixtures.RegisterRequest request = TestFixtures.register(suffix);

        Response register = client.register(request);
        assertEquals(200, register.statusCode());

        // 로그인 성공은 auth-service enabled=true 전환이 끝났다는 의미다.
        Response login = PollingSupport.pollUntil(
                Duration.ofSeconds(15),
                Duration.ofMillis(500),
                () -> client.login(request.loginId(), request.password()),
                response -> response.statusCode() == 200
        );

        String accessToken = E2EExtractors.accessToken(login);

        // 이후 /me 조회가 성공하면 user-service profile 생성도 끝난 상태다.
        Response me = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(300),
                () -> client.getMe(accessToken),
                response -> response.statusCode() == 200
        );

        assertEquals(200, me.statusCode());
    }

    @Test
    void register_duplicate_email_should_fail() {
        String suffix = String.valueOf(System.currentTimeMillis());
        TestFixtures.RegisterRequest first = TestFixtures.register(suffix);
        TestFixtures.RegisterRequest second = new TestFixtures.RegisterRequest(
                first.email(),
                "another-" + suffix,
                first.password(),
                first.userName(),
                first.empNo(),
                first.pstnName(),
                first.tel()
        );

        assertEquals(200, client.register(first).statusCode());
        assertEquals(409, client.register(second).statusCode());
    }

    @Test
    void register_duplicate_login_id_should_fail() {
        String suffix = String.valueOf(System.currentTimeMillis());
        TestFixtures.RegisterRequest first = TestFixtures.register(suffix);
        TestFixtures.RegisterRequest second = new TestFixtures.RegisterRequest(
                "another-" + suffix + "@example.com",
                first.loginId(),
                first.password(),
                first.userName(),
                first.empNo(),
                first.pstnName(),
                first.tel()
        );

        assertEquals(200, client.register(first).statusCode());
        assertEquals(409, client.register(second).statusCode());
    }
}
