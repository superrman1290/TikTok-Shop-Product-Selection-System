package com.tiktokinsight.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class SecureHashing {

    private final SecretKeySpec refreshHashKey;

    public SecureHashing(AuthProperties properties) {
        this.refreshHashKey = new SecretKeySpec(
                properties.refreshSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    public String refreshTokenHash(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(refreshHashKey);
            return toHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash refresh token", exception);
        }
    }

    public String identifierHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String toHex(byte[] bytes) {
        return java.util.HexFormat.of().formatHex(bytes);
    }
}
