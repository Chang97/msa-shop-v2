package com.msashop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msashop.auth.command.adapter.out.persistence.entity.RefreshTokenEntity;
import com.msashop.auth.command.adapter.out.persistence.entity.UserEntity;
import com.msashop.auth.command.adapter.out.persistence.repo.RefreshTokenJpaRepository;
import com.msashop.auth.command.adapter.out.persistence.repo.UserJpaRepository;
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
public class AuthFlowIntegrationTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserJpaRepository userJpaRepository;
    @Autowired
    RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 테스트 격리: 기존 토큰/유저 정리 (FK 때문에 refresh 먼저)
//        refreshTokenJpaRepository.deleteAll();
        refreshTokenJpaRepository.truncateAll();;
        userJpaRepository.deleteAll();

        // 테스트 유저 삽입(로그인 대상)
        UserEntity user = UserEntity.builder()
                .email("test@test.com")
                .loginId("test")
                .userPassword(passwordEncoder.encode("1234"))
                .userName("테스트")
                .useYn(true)
                .build();

        UserEntity saved = userJpaRepository.save(user);
        this.testUserId = saved.getUserId();
    }

    @Test
    void login_refresh_rotate_logout_flow() throws Exception {
        // 1) 로그인
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
        assertThat(setCookie1).contains("rt=").contains("HttpOnly"); // refresh cookie 존재

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String access1 = loginJson.get("accessToken").asText();
        assertThat(access1).isNotBlank();

        // 쿠키 파싱(MockMvc는 헤더 문자열에서 직접 파싱이 번거로워서, 간단히 rt=값 추출)
        String rt1 = extractCookieValue(setCookie1, "rt");
        String tokenId1 = extractTokenId(rt1);

        // DB: login 후 refresh_token 1개 생성
        assertThat(refreshTokenJpaRepository.countByUserId(testUserId)).isEqualTo(1);
        RefreshTokenEntity t1 = refreshTokenJpaRepository.findByTokenId(tokenId1).orElseThrow();
        assertThat(Boolean.TRUE.equals(t1.getRevoked())).isFalse();

        MockCookie refreshCookie1 = new MockCookie("rt", rt1);
        refreshCookie1.setPath("/api/auth");;

        // 2) refresh(rotate)
        var refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie1))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        String setCookie2 = refreshResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String rt2 = extractCookieValue(setCookie2, "rt");
        assertThat(rt2).isNotBlank();
        assertThat(rt2).isNotEqualTo(rt1); // rotate 확인

        String tokenId2 = extractTokenId(rt2);

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String access2 = refreshJson.get("accessToken").asText();
        assertThat(access2).isNotBlank();

        // DB: rotate 후 t1 revoked=true, replaced_by_token_id = tokenId2, t2 존재
        RefreshTokenEntity t1After = refreshTokenJpaRepository.findByTokenId(tokenId1).orElseThrow();
        RefreshTokenEntity t2 = refreshTokenJpaRepository.findByTokenId(tokenId2).orElseThrow();

        assertThat(Boolean.TRUE.equals(t1After.getRevoked())).isTrue();
        assertThat(t1After.getReplacedByTokenId()).isEqualTo(tokenId2);
        assertThat(Boolean.TRUE.equals(t2.getRevoked())).isFalse();

        // 3) 이전 rt로 refresh 재시도 -> 409 + code
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie1))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value("AUTH_013"));


        // 4) logout (최신 rt로)
        MockCookie refreshCookie2 = new MockCookie("rt", rt2);
        refreshCookie2.setPath("/api/auth");

        var logoutResult = mockMvc.perform(post("/api/auth/logout")
                .cookie(refreshCookie2))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String setCookieLogout = logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieLogout).contains("rt=").contains("Max-Age=0"); // 삭제 쿠키

        // DB: logout 후 t2 revoked=true
        RefreshTokenEntity t2After = refreshTokenJpaRepository.findByTokenId(tokenId2).orElseThrow();
        assertThat(Boolean.TRUE.equals(t2After.getRevoked())).isTrue();

        // 5) Logout 이후 refresh 시도 -> 401
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie2))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_012"));

    }

    private static String extractCookieValue(String setCookieHeader, String cookieName) {
        // 예: "rt=abc.def; Path=/api/auth; Max-Age=..."
        String prefix = cookieName + "=";
        int start = setCookieHeader.indexOf(prefix);
        if (start < 0) return null;
        start += prefix.length();
        int end = setCookieHeader.indexOf(';', start);
        return (end > start) ? setCookieHeader.substring(start, end) : setCookieHeader.substring(start);
    }

    private static String extractTokenId(String rawRefreshToken) {
        int idx = rawRefreshToken.indexOf('.');
        return rawRefreshToken.substring(0, idx);
    }

}
