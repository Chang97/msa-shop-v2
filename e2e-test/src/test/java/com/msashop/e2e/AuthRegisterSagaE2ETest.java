package com.msashop.e2e;

import com.msashop.e2e.support.E2EClient;
import com.msashop.e2e.support.E2EExtractors;
import com.msashop.e2e.support.PollingSupport;
import com.msashop.e2e.support.TestFixtures;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// 회원가입 Saga 가 인증 가능 상태와 프로필 생성 상태로 최종 수렴하는지 검증한다.
public class AuthRegisterSagaE2ETest {
    private final E2EClient client = new E2EClient();

    // 회원가입 성공 후 Saga 완료 시점에 로그인이 가능해지는지 검증한다.
    @Test
    void register_success_should_eventually_enable_login() {
        String suffix = String.valueOf(System.currentTimeMillis());
        TestFixtures.RegisterRequest request = TestFixtures.register(suffix);

        // 회원가입 API 는 로컬 저장과 이벤트 적재까지 끝나면 즉시 200 을 반환한다.
        Response register = client.register(request);
        assertEquals(200, register.statusCode());

        // Saga 완료 전에는 로그인에 실패할 수 있으므로 polling 으로 최종 성공을 기다린다.
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

    // 회원가입 성공 후 user-service 프로필이 최종적으로 생성되는지 검증한다.
    @Test
    void register_success_should_eventually_create_profile() {
        String suffix = String.valueOf(System.nanoTime());
        TestFixtures.RegisterRequest request = TestFixtures.register(suffix);

        Response register = client.register(request);
        assertEquals(200, register.statusCode());

        // 로그인 성공은 auth-service 측 활성화가 완료되었음을 의미한다.
        Response login = PollingSupport.pollUntil(
                Duration.ofSeconds(15),
                Duration.ofMillis(500),
                () -> client.login(request.loginId(), request.password()),
                response -> response.statusCode() == 200
        );

        String accessToken = E2EExtractors.accessToken(login);

        // 이후 /me 조회가 성공하면 user-service 프로필 생성도 완료된 상태다.
        Response me = PollingSupport.pollUntil(
                Duration.ofSeconds(10),
                Duration.ofMillis(300),
                () -> client.getMe(accessToken),
                response -> response.statusCode() == 200
        );

        assertEquals(200, me.statusCode());
    }

    // 이미 사용 중인 이메일로는 회원가입할 수 없는지 검증한다.
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

    // 이미 사용 중인 로그인 ID 로는 회원가입할 수 없는지 검증한다.
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
