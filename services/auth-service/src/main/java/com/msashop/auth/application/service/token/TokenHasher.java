package com.msashop.auth.application.service.token;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * ?좏겙 ?먮Ц(raw)???덉쟾?섍쾶 ??ν븯湲??꾪빐 ?댁떆濡?蹂?섑븯??而댄룷?뚰듃.
 *
 * 1李⑥뿉?쒕뒗 SHA-256(hex)???ъ슜:
 * - ???媛?湲몄씠: 64 chars (hex)
 * - token_hash 而щ읆 湲몄씠(512)??異⑸텇???ㅼ뼱媛?
 *
 * 李멸퀬:
 * - ?댁쁺?먯꽌??HMAC(SHA-256 + ?쒕쾭 ?쒗겕由?濡?留뚮뱾硫?"DB ?좎텧 ???ㅽ봽?쇱씤 ???怨듦꺽"????以꾩씪 ???덉쓬
 * - 濡쒖뺄 ?④퀎?먯꽌??SHA-256留뚯쑝濡쒕룄 ?ㅺ퀎 ?섎룄 ?꾨떖 媛??
 */
@Component
public class TokenHasher {

    private static final HexFormat HEX = HexFormat.of();

    public String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to hash token", e);
        }
    }
}

