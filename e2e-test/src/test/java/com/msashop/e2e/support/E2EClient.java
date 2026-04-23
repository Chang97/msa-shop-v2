package com.msashop.e2e.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * E2E 테스트용 단순 HTTP 클라이언트 헬퍼.
 * - 기본 베이스 URL: http://localhost:8080
 * - JSON 직렬화: Jackson ObjectMapper
 * - HTTP 호출: RestAssured
 */
public class E2EClient {

    private static final String BASE_URL = "http://localhost:8080";
    private static final int MAX_RATE_LIMIT_RETRIES = 3;
    private static final long RATE_LIMIT_RETRY_SLEEP_MILLIS = 1_100L;
    private static String cachedAdminToken;
    private static String cachedUserToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * admin/1234 계정으로 로그인해 accessToken을 반환.
     */
    public String loginAdminToken() {
        return cachedLoginToken(TestFixtures.adminLogin(), true);
    }

    /**
     * user1/1234 계정으로 로그인해 accessToken을 반환.
     */
    public String loginUserToken() {
        return cachedLoginToken(TestFixtures.userLogin(), false);
    }

    /**
     * 로그인 요청을 보내고 accessToken 문자열을 추출한다.
     * 성공/실패 여부는 호출 측 테스트에서 상태코드로 검증한다.
     */
    public String loginAndGetAccessToken(String loginId, String password) {
        Response response = postJson(
                "/api/auth/login",
                null,
                new TestFixtures.LoginRequest(loginId, password)
        );
        return E2EExtractors.accessToken(response);
    }

    /**
     * JSON POST 헬퍼.
     * - Authorization 헤더는 토큰이 비어있지 않을 때만 추가한다.
     */
    public Response postJson(String path, String accessTokenOrNull, Object body) {
        String jsonBody = write(body);
        for (int attempt = 0; ; attempt++) {
            var request = RestAssured.given()
                    .baseUri(BASE_URL)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            if (accessTokenOrNull != null && !accessTokenOrNull.isBlank()) {
                request.header("Authorization", "Bearer " + accessTokenOrNull);
            }

            Response response = request
                    .body(jsonBody)
                    .when()
                    .post(path)
                    .andReturn();

            if (!shouldRetryAfterRateLimit(path, response, attempt)) {
                return response;
            }
            sleepRateLimitWindow();
        }
    }

    /**
     * GET 헬퍼.
     * - Authorization 헤더는 토큰이 비어있지 않을 때만 추가한다.
     */
    public Response get(String path, String accessTokenOrNull) {
        var request = RestAssured.given()
                .baseUri(BASE_URL)
                .header("Accept", "application/json");
        if (accessTokenOrNull != null && !accessTokenOrNull.isBlank()) {
            request.header("Authorization", "Bearer " + accessTokenOrNull);
        }
        return request
                .when()
                .get(path)
                .andReturn();
    }

    private String write(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("요청 본문 직렬화에 실패했습니다.", e);
        }
    }

    public Response postWithCookie(String path, String cookieName, String cookieValue) {
        for (int attempt = 0; ; attempt++) {
            Response response = RestAssured.given()
                    .baseUri(BASE_URL)
                    .accept("application/json")
                    .cookie(cookieName, cookieValue)
                    .when()
                    .post(path)
                    .andReturn();

            if (!shouldRetryAfterRateLimit(path, response, attempt)) {
                return response;
            }
            sleepRateLimitWindow();
        }
    }

    /**
     * 회원가입 요청을 gateway를 통해 보낸다.
     * saga는 비동기지만 register API 자체는 즉시 응답하므로 Response를 그대로 반환한다.
     */
    public Response register(TestFixtures.RegisterRequest request) {
        return postJson("/api/auth/register", null, request);
    }

    /**
     * 로그인 요청 helper.
     * saga 완료 전에는 실패할 수 있으므로 polling과 함께 쓰기 좋다.
     */
    public Response login(String loginId, String password) {
        return postJson(
                "/api/auth/login",
                null,
                new TestFixtures.LoginRequest(loginId, password)
        );
    }

    /**
     * 현재 로그인한 사용자의 내 정보 조회.
     * 회원가입 saga가 끝난 뒤 user-service profile이 준비되었는지 검증할 때 사용한다.
     */
    public Response getMe(String accessToken) {
        return get("/api/users/me", accessToken);
    }

    private synchronized String cachedLoginToken(TestFixtures.LoginRequest request, boolean admin) {
        String cached = admin ? cachedAdminToken : cachedUserToken;
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        String issued = loginAndGetAccessToken(request.loginId(), request.password());
        if (admin) {
            cachedAdminToken = issued;
        } else {
            cachedUserToken = issued;
        }
        return issued;
    }

    private boolean shouldRetryAfterRateLimit(String path, Response response, int attempt) {
        return response != null
                && response.statusCode() == 429
                && attempt < MAX_RATE_LIMIT_RETRIES
                && ("/api/auth/login".equals(path)
                || "/api/auth/refresh".equals(path)
                || path.matches("/api/orders/[^/]+/pay"));
    }

    private void sleepRateLimitWindow() {
        try {
            Thread.sleep(RATE_LIMIT_RETRY_SLEEP_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("rate limit 재시도 대기 중 인터럽트가 발생했습니다.", e);
        }
    }
}
