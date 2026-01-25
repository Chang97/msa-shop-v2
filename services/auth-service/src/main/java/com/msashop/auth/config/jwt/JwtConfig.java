package com.msashop.auth.config.jwt;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Auth 서비스에서 RS256 JWT를 "발급"하기 위한 설정.
 *
 * 핵심:
 * - RS256은 "비대칭키 서명"이므로 Auth는 private key로 서명(JWT 발급)하고
 * - Gateway/다른 서비스는 public key로 검증만 수행
 *
 * 여기서는 Nimbus(JOSE 라이브러리)를 사용해 JwtEncoder를 구성한다.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties props) throws Exception {
        // PEM 파일을 읽어서 JDK RSA Key 객체로 변환
        RSAPrivateKey priv = (RSAPrivateKey) readPrivateKey(props.privateKeyLocation());
        RSAPublicKey pub = (RSAPublicKey) readPublicKey(props.publicKeyLocation());
        /*
         * JWK(JSON Web Key): 키를 JSON 표현으로 다루기 위한 표준 포맷.
         * NimbusJwtEncoder는 JWKSource를 받아 서명에 사용한다.
         *
         * - pub: 검증 및 (추후 JWKS 제공 시) 노출 가능
         * - priv: 절대 외부 노출 금지 (Auth 내부에서만 보관)
         */
        JWK jwk = new RSAKey.Builder(pub).privateKey(priv).build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * PEM(텍스트) -> DER(bytes) -> RSAPrivateKey 변환
     *
     * PEM은 "-----BEGIN PRIVATE KEY-----" 헤더/푸터 + 개행이 포함된 텍스트 포맷이라
     * KeyFactory로 읽기 전에:
     *  1) 헤더/푸터 제거
     *  2) 공백/개행 제거
     *  3) Base64 디코딩
     * 과정이 필요하다.
     *
     * 주의: 이 코드는 PKCS#8 ("BEGIN PRIVATE KEY") 형태를 전제로 한다.
     */
    private PrivateKey readPrivateKey(Resource resource) throws Exception {
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    /**
     * PEM(텍스트) -> DER(bytes) -> RSAPublicKey 변환
     *
     * PEM은 "-----BEGIN PUBLIC KEY-----" 헤더/푸터 + 개행이 포함된 텍스트 포맷이라
     *      * KeyFactory로 읽기 전에:
     *      *  1) 헤더/푸터 제거
     *      *  2) 공백/개행 제거
     *      *  3) Base64 디코딩
     *      * 과정이 필요하다.
     *
     * 주의: 이 코드는 X.509 SubjectPublicKeyInfo ("BEGIN PUBLIC KEY") 형태를 전제로 한다.
     */
    private PublicKey readPublicKey(Resource resource) throws Exception {
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }
}
