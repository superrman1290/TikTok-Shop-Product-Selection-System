package com.tiktokinsight.auth.infrastructure;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.auth.application.IssuedTokens;
import com.tiktokinsight.auth.domain.UserAccount;
import com.tiktokinsight.auth.domain.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final AuthProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public TokenService(AuthProperties properties) {
        this.properties = properties;
        SecretKeySpec key = new SecretKeySpec(
                properties.accessSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        this.decoder = jwtDecoder;
    }

    public IssuedTokens issue(UserAccount user, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(Long.toString(user.id()))
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("token_type", "access")
                .claim("email", user.email())
                .claim("username", user.username())
                .claim("role", user.role().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        byte[] refreshBytes = new byte[48];
        SECURE_RANDOM.nextBytes(refreshBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(refreshBytes);
        return new IssuedTokens(accessToken, refreshToken, expiresAt);
    }

    public AccessPrincipal decodeAccessToken(String token) {
        Jwt jwt = decoder.decode(token);
        if (!"access".equals(jwt.getClaimAsString("token_type"))) {
            throw new IllegalArgumentException("Unexpected token type");
        }
        return new AccessPrincipal(
                Long.parseLong(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("username"),
                UserRole.valueOf(jwt.getClaimAsString("role"))
        );
    }

    public Instant refreshExpiresAt(Instant issuedAt) {
        return issuedAt.plus(properties.refreshTokenTtl());
    }
}
