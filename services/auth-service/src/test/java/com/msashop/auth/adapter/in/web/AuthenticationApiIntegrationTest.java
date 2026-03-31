package com.msashop.auth.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.adapter.in.web.cookie.RefreshCookieFactory;
import com.msashop.auth.adapter.in.web.dto.LoginRequest;
import com.msashop.auth.adapter.in.web.dto.RegisterRequest;
import com.msashop.auth.adapter.out.persistence.entity.AuthUserCredentialJpaEntity;
import com.msashop.auth.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.msashop.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import com.msashop.auth.adapter.out.persistence.entity.RoleJpaEntity;
import com.msashop.auth.adapter.out.persistence.entity.UserRoleMapJpaEntity;
import com.msashop.auth.adapter.out.persistence.repo.AuthUserCredentialJpaRepository;
import com.msashop.auth.adapter.out.persistence.repo.OutboxEventJpaRepository;
import com.msashop.auth.adapter.out.persistence.repo.RefreshTokenJpaRepository;
import com.msashop.auth.adapter.out.persistence.repo.RoleJpaRepository;
import com.msashop.auth.adapter.out.persistence.repo.UserRoleMapJpaRepository;
import com.msashop.auth.application.service.token.TokenHasher;
import com.msashop.common.event.EventTypes;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.kafka.consumers.auth-user-saga-enabled=false",
        "app.kafka.producer.relay-enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthenticationApiIntegrationTest {

    private static final Pattern REFRESH_COOKIE_PATTERN = Pattern.compile("^" + RefreshCookieFactory.REFRESH_COOKIE_NAME + "=([^;]+)");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_service_api_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenHasher tokenHasher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthUserCredentialJpaRepository credentialJpaRepository;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Autowired
    private RoleJpaRepository roleJpaRepository;

    @Autowired
    private UserRoleMapJpaRepository userRoleMapJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @AfterEach
    void tearDown() {
        // 각 테스트가 완전히 독립적으로 동작하도록 핵심 테이블만 비운다.
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    user_role_map,
                    "role",
                    auth_refresh_token,
                    auth_user_credential,
                    outbox_event,
                    processed_event
                RESTART IDENTITY CASCADE
                """);
    }

    @Test
    @DisplayName("로그인 성공 시 access token과 refresh cookie가 발급되고 refresh token hash가 저장된다")
    void should_login_and_issue_tokens() throws Exception {
        AuthUserCredentialJpaEntity credential = credentialJpaRepository.save(AuthUserCredentialJpaEntity.builder()
                .email("test@example.com")
                .loginId("tester")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .enabled(true)
                .build());
        Integer roleId = roleJpaRepository.save(RoleJpaEntity.builder()
                .roleName("ROLE_USER")
                .useYn(true)
                .build()).getRoleId();
        userRoleMapJpaRepository.save(UserRoleMapJpaEntity.builder()
                .userId(credential.getUserId())
                .roleId(roleId)
                .build());

        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("tester", "pass1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(cookie().httpOnly(RefreshCookieFactory.REFRESH_COOKIE_NAME, true))
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        String refreshToken = extractRefreshToken(setCookie);
        String refreshHash = tokenHasher.sha256Hex(refreshToken);

        Optional<RefreshTokenEntity> stored = refreshTokenJpaRepository.findByTokenHash(refreshHash);
        assertThat(stored).isPresent();
        assertThat(stored.orElseThrow().getUserId()).isEqualTo(credential.getUserId());
        assertThat(stored.orElseThrow().getRevoked()).isFalse();
    }

    @Test
    @DisplayName("비활성 계정은 로그인할 수 없다")
    void should_reject_disabled_user_login() throws Exception {
        credentialJpaRepository.save(AuthUserCredentialJpaEntity.builder()
                .email("disabled@example.com")
                .loginId("disabled-user")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .enabled(false)
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("disabled-user", "pass1234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("refresh 성공 시 기존 refresh token은 revoke되고 새 토큰이 발급된다")
    void should_refresh_and_rotate_refresh_token() throws Exception {
        AuthUserCredentialJpaEntity credential = credentialJpaRepository.save(AuthUserCredentialJpaEntity.builder()
                .email("refresh@example.com")
                .loginId("refresh-user")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .enabled(true)
                .build());
        Integer roleId = roleJpaRepository.save(RoleJpaEntity.builder()
                .roleName("ROLE_USER")
                .useYn(true)
                .build()).getRoleId();
        userRoleMapJpaRepository.save(UserRoleMapJpaEntity.builder()
                .userId(credential.getUserId())
                .roleId(roleId)
                .build());

        String firstCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("refresh-user", "pass1234"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        String oldRefreshToken = extractRefreshToken(firstCookie);
        String oldRefreshHash = tokenHasher.sha256Hex(oldRefreshToken);

        String secondCookie = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(RefreshCookieFactory.REFRESH_COOKIE_NAME, oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        String newRefreshToken = extractRefreshToken(secondCookie);
        String newRefreshHash = tokenHasher.sha256Hex(newRefreshToken);

        assertThat(newRefreshHash).isNotEqualTo(oldRefreshHash);
        assertThat(refreshTokenJpaRepository.findByTokenHash(oldRefreshHash)).isPresent();
        assertThat(refreshTokenJpaRepository.findByTokenHash(oldRefreshHash).orElseThrow().getRevoked()).isTrue();
        assertThat(refreshTokenJpaRepository.findByTokenHash(newRefreshHash)).isPresent();
        assertThat(refreshTokenJpaRepository.findByTokenHash(newRefreshHash).orElseThrow().getRevoked()).isFalse();
        assertThat(refreshTokenJpaRepository.countByUserId(credential.getUserId())).isEqualTo(2);
    }

    @Test
    @DisplayName("logout 성공 시 refresh token은 revoke되고 삭제 쿠키가 내려간다")
    void should_logout_and_revoke_refresh_token() throws Exception {
        AuthUserCredentialJpaEntity credential = credentialJpaRepository.save(AuthUserCredentialJpaEntity.builder()
                .email("logout@example.com")
                .loginId("logout-user")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .enabled(true)
                .build());
        Integer roleId = roleJpaRepository.save(RoleJpaEntity.builder()
                .roleName("ROLE_USER")
                .useYn(true)
                .build()).getRoleId();
        userRoleMapJpaRepository.save(UserRoleMapJpaEntity.builder()
                .userId(credential.getUserId())
                .roleId(roleId)
                .build());

        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("logout-user", "pass1234"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        String refreshToken = extractRefreshToken(setCookie);
        String refreshHash = tokenHasher.sha256Hex(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie(RefreshCookieFactory.REFRESH_COOKIE_NAME, refreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        assertThat(refreshTokenJpaRepository.findByTokenHash(refreshHash)).isPresent();
        assertThat(refreshTokenJpaRepository.findByTokenHash(refreshHash).orElseThrow().getRevoked()).isTrue();
    }

    @Test
    @DisplayName("회원가입 성공 시 disabled credential, 기본 권한, outbox 이벤트가 함께 저장된다")
    void should_register_and_persist_role_and_outbox() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "register@example.com",
                                "register-user",
                                "pass1234",
                                "홍길동",
                                "EMP-001",
                                "개발자",
                                "010-1234-5678"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.registeredAt").isString());

        AuthUserCredentialJpaEntity credential = credentialJpaRepository.findByLoginId("register-user").orElseThrow();
        assertThat(credential.getEnabled()).isFalse();
        assertThat(userRoleMapJpaRepository.findRoleNamesByUserId(credential.getUserId())).contains("ROLE_USER");
        assertThat(outboxEventJpaRepository.findAll())
                .singleElement()
                .extracting(OutboxEventJpaEntity::getEventType)
                .isEqualTo(EventTypes.AUTH_USER_CREATED);
    }

    @Test
    @DisplayName("auth/me 조회 성공 시 gateway 헤더의 사용자 기준으로 인증 프로필을 반환한다")
    void should_return_auth_profile_for_authenticated_user() throws Exception {
        // 인증 계정과 권한을 먼저 저장해 auth-service가 조회할 대상을 만든다.
        AuthUserCredentialJpaEntity credential = credentialJpaRepository.save(AuthUserCredentialJpaEntity.builder()
                .email("me@example.com")
                .loginId("me-user")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .enabled(true)
                .build());
        Integer roleId = roleJpaRepository.save(RoleJpaEntity.builder()
                .roleName("ROLE_USER")
                .useYn(true)
                .build()).getRoleId();
        userRoleMapJpaRepository.save(UserRoleMapJpaEntity.builder()
                .userId(credential.getUserId())
                .roleId(roleId)
                .build());

        // gateway가 붙여주는 인증 헤더를 흉내 내서 현재 사용자 컨텍스트를 만든다.
        mockMvc.perform(get("/api/auth/me")
                        .header("X-User-Id", credential.getUserId())
                        .header("X-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(credential.getUserId()))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.loginId").value("me-user"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    private String extractRefreshToken(String setCookie) {
        Matcher matcher = REFRESH_COOKIE_PATTERN.matcher(setCookie);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
