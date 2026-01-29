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
 * Auth ?쒕퉬?ㅼ뿉??RS256 JWT瑜?"諛쒓툒"?섍린 ?꾪븳 ?ㅼ젙.
 *
 * ?듭떖:
 * - RS256? "鍮꾨?移?궎 ?쒕챸"?대?濡?Auth??private key濡??쒕챸(JWT 諛쒓툒)?섍퀬
 * - Gateway/?ㅻⅨ ?쒕퉬?ㅻ뒗 public key濡?寃利앸쭔 ?섑뻾
 *
 * ?ш린?쒕뒗 Nimbus(JOSE ?쇱씠釉뚮윭由?瑜??ъ슜??JwtEncoder瑜?援ъ꽦?쒕떎.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties props) throws Exception {
        // PEM ?뚯씪???쎌뼱??JDK RSA Key 媛앹껜濡?蹂??
        RSAPrivateKey priv = (RSAPrivateKey) readPrivateKey(props.privateKeyLocation());
        RSAPublicKey pub = (RSAPublicKey) readPublicKey(props.publicKeyLocation());
        /*
         * JWK(JSON Web Key): ?ㅻ? JSON ?쒗쁽?쇰줈 ?ㅻ（湲??꾪븳 ?쒖? ?щ㎎.
         * NimbusJwtEncoder??JWKSource瑜?諛쏆븘 ?쒕챸???ъ슜?쒕떎.
         *
         * - pub: 寃利?諛?(異뷀썑 JWKS ?쒓났 ?? ?몄텧 媛??
         * - priv: ?덈? ?몃? ?몄텧 湲덉? (Auth ?대??먯꽌留?蹂닿?)
         */
        JWK jwk = new RSAKey.Builder(pub).privateKey(priv).build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * PEM(?띿뒪?? -> DER(bytes) -> RSAPrivateKey 蹂??
     *
     * PEM? "-----BEGIN PRIVATE KEY-----" ?ㅻ뜑/?명꽣 + 媛쒗뻾???ы븿???띿뒪???щ㎎?대씪
     * KeyFactory濡??쎄린 ?꾩뿉:
     *  1) ?ㅻ뜑/?명꽣 ?쒓굅
     *  2) 怨듬갚/媛쒗뻾 ?쒓굅
     *  3) Base64 ?붿퐫??
     * 怨쇱젙???꾩슂?섎떎.
     *
     * 二쇱쓽: ??肄붾뱶??PKCS#8 ("BEGIN PRIVATE KEY") ?뺥깭瑜??꾩젣濡??쒕떎.
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
     * PEM(?띿뒪?? -> DER(bytes) -> RSAPublicKey 蹂??
     *
     * PEM? "-----BEGIN PUBLIC KEY-----" ?ㅻ뜑/?명꽣 + 媛쒗뻾???ы븿???띿뒪???щ㎎?대씪
     *      * KeyFactory濡??쎄린 ?꾩뿉:
     *      *  1) ?ㅻ뜑/?명꽣 ?쒓굅
     *      *  2) 怨듬갚/媛쒗뻾 ?쒓굅
     *      *  3) Base64 ?붿퐫??
     *      * 怨쇱젙???꾩슂?섎떎.
     *
     * 二쇱쓽: ??肄붾뱶??X.509 SubjectPublicKeyInfo ("BEGIN PUBLIC KEY") ?뺥깭瑜??꾩젣濡??쒕떎.
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
