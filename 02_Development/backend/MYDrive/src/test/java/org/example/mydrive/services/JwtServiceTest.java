package org.example.mydrive.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "0123456789-abcdefghijklmnopqrstuvwxyz-secret-key";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    void generateToken_setsEmailAsSubject() {
        String token = jwtService.generateToken(7L, "user@example.com");

        assertThat(parse(token).getSubject()).isEqualTo("user@example.com");
    }

    @Test
    void generateToken_storesUserIdAsUidClaim() {
        String token = jwtService.generateToken(123L, "u@e.com");

        Claims claims = parse(token);
        assertThat(claims.get("uid", Long.class)).isEqualTo(123L);
    }

    @Test
    void generateToken_setsIssuedAtAndExpiry() {
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken(1L, "u@e.com");

        Claims claims = parse(token);
        Date issuedAt = claims.getIssuedAt();
        Date expiry = claims.getExpiration();

        assertThat(issuedAt).isNotNull();
        assertThat(expiry).isNotNull();
        assertThat(expiry).isAfter(issuedAt);
        // expiry ~ issuedAt + 1h (allow clock granularity since JWT trims to seconds)
        long deltaMs = expiry.getTime() - issuedAt.getTime();
        assertThat(deltaMs).isEqualTo(3_600_000L);
        assertThat(expiry.getTime()).isGreaterThan(before);
    }

    @Test
    void generateToken_isVerifiableWithTheConfiguredSecret() {
        String token = jwtService.generateToken(5L, "verify@example.com");

        // parse() throws if the signature does not verify
        assertThat(parse(token).getSubject()).isEqualTo("verify@example.com");
    }
}
