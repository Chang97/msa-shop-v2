package com.msashop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.adapter.out.persistence.entity.AuthUserCredentialJpaEntity;
import com.msashop.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import com.msashop.auth.adapter.out.persistence.repo.AuthUserCredentialJpaRepository;
import com.msashop.auth.adapter.out.persistence.repo.RefreshTokenJpaRepository;
import com.msashop.auth.application.service.token.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AuthUserCredentialJpaRepository credentialRepository;
    @Autowired
    RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    TokenHasher tokenHasher;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        refreshTokenJpaRepository.truncateAll();
        credentialRepository.deleteAll();

        AuthUserCredentialJpaEntity user = AuthUserCredentialJpaEntity.builder()
                .email("test@test.com")
                .loginId("test")
                .passwordHash(passwordEncoder.encode("1234"))
                .enabled(true)
                .build();

        AuthUserCredentialJpaEntity saved = credentialRepository.save(user);
        this.testUserId = saved.getUserId();
    }

    @Test
    void login_refresh_rotate_logout_flow() throws Exception {
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"loginId":"test","password":"1234"}
                            """))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        String setCookie1 = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie1).contains("rt=").contains("HttpOnly");

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String access1 = loginJson.get("accessToken").asText();
        assertThat(access1).isNotBlank();

        String rt1 = extractCookieValue(setCookie1, "rt");
        String hash1 = tokenHasher.sha256Hex(rt1);

        assertThat(refreshTokenJpaRepository.countByUserId(testUserId)).isEqualTo(1);
        RefreshTokenEntity t1 = refreshTokenJpaRepository.findByTokenHash(hash1).orElseThrow();
        assertThat(Boolean.TRUE.equals(t1.getRevoked())).isFalse();

        MockCookie refreshCookie1 = new MockCookie("rt", rt1);
        refreshCookie1.setPath("/api/auth");

        var refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie1))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        String setCookie2 = refreshResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String rt2 = extractCookieValue(setCookie2, "rt");
        assertThat(rt2).isNotBlank();
        assertThat(rt2).isNotEqualTo(rt1);

        String hash2 = tokenHasher.sha256Hex(rt2);

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String access2 = refreshJson.get("accessToken").asText();
        assertThat(access2).isNotBlank();

        RefreshTokenEntity t1After = refreshTokenJpaRepository.findByTokenHash(hash1).orElseThrow();
        RefreshTokenEntity t2 = refreshTokenJpaRepository.findByTokenHash(hash2).orElseThrow();

        assertThat(Boolean.TRUE.equals(t1After.getRevoked())).isTrue();
        assertThat(Boolean.TRUE.equals(t2.getRevoked())).isFalse();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie1))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value("AUTH_012"));

        MockCookie refreshCookie2 = new MockCookie("rt", rt2);
        refreshCookie2.setPath("/api/auth");

        var logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie2))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookieLogout = logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieLogout).contains("rt=").contains("Max-Age=0");

        RefreshTokenEntity t2After = refreshTokenJpaRepository.findByTokenHash(hash2).orElseThrow();
        assertThat(Boolean.TRUE.equals(t2After.getRevoked())).isTrue();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie2))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_012"));

    }

    private static String extractCookieValue(String setCookieHeader, String cookieName) {
        String prefix = cookieName + "=";
        int start = setCookieHeader.indexOf(prefix);
        if (start < 0) return null;
        start += prefix.length();
        int end = setCookieHeader.indexOf(';', start);
        return (end > start) ? setCookieHeader.substring(start, end) : setCookieHeader.substring(start);
    }
}

