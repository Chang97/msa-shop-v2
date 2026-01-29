package com.msashop.auth.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * auth-service 蹂댁븞 ?ㅼ젙(理쒖냼 援ъ꽦)
 *
 * ??븷:
 * - auth-service??"諛쒓툒/?뚯쟾/?먭린"留??대떦
 * - access token 寃利?Resource Server)? Gateway?먯꽌 ?섑뻾
 *
 * ?뺤콉:
 * - ?몄뀡/?쇰줈洹몄씤/Basic 鍮꾪솢?깊솕
 * - CSRF??auth API????댁꽌??disable(?좏겙 湲곕컲 + refresh??HttpOnly cookie)
 * - /api/auth/login, /api/auth/refresh, /api/auth/logout 留?怨듦컻
 * - ?섎㉧吏 ?붿껌? 404/403濡?留됱븘 ?쒕퉬??硫댁쟻 異뺤냼
 *
 * ?대? ?쒕퉬???몄텧(X-Internal-Secret) 媛숈? 嫄??섏쨷??蹂꾨룄 ?꾪꽣濡?異붽??좎닔??
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ?몄뀡 誘몄궗??
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 湲곕낯 濡쒓렇??踰좎씠吏??몄쬆 ?꾧린 (?닿굅 ???꾨㈃ ?섎룄移??딄쾶 401 ?좊컻?섎뒗 寃쎌슦 留롮쓬)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable) // /api/auth/logout 吏곸젒 援ы쁽

                // CSRF: auth API??stateless + cookie refresh吏留?SameSite/HttpOnly濡?諛⑹뼱
                .csrf(AbstractHttpConfigurer::disable)

                // CORS??gateway?먯꽌 泥섎━
                .cors(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // 濡쒓렇??媛깆떊/濡쒓렇?꾩썐
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        // ?댁쁺 ?꾩슂 ??
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 洹??몃뒗 ?꾨? 李⑤떒 (auth-service??梨낆엫 踰붿쐞 異뺤냼)
                        .anyRequest().denyAll()
                )
                .build();
    }
}
