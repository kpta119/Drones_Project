package com.example.drones.config;

import com.example.drones.common.config.JwtService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTests {

    @Mock
    private Clock clock;

    @InjectMocks
    private JwtService jwtService;

    private static final String TEST_SECRET_KEY = "12345d67890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
    private static final long TEST_JWT_EXPIRATION = 3600000;
    private UUID testUserId;
    private final Instant fixedInstant = Instant.parse("2023-01-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_JWT_EXPIRATION);
        jwtService.init();
        testUserId = UUID.randomUUID();
        lenient().when(clock.instant()).thenReturn(fixedInstant);
    }

    @Test
    void givenUserId_whenGenerateToken_thenTokenIsCreated() {
        String token = jwtService.generateToken(testUserId);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void givenToken_whenExtractUserId_thenCorrectUserIdIsReturned() {
        String token = jwtService.generateToken(testUserId);
        UUID extractedUserId = jwtService.extractUserId(token);
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    void givenValidToken_whenIsTokenValid_thenReturnsTrue() {
        String token = jwtService.generateToken(testUserId);
        boolean isValid = jwtService.isTokenValid(token);
        assertThat(isValid).isTrue();
    }

    @Test
    void givenExpiredToken_whenIsTokenValid_thenReturnsFalse() {
        String expiredToken = createExpiredToken(testUserId);
        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void givenValidToken_whenRefreshTokenIfNeeded_thenReturnsSameToken() {
        String token = jwtService.generateToken(testUserId);
        String refreshedToken = jwtService.refreshTokenIfNeeded(token);
        assertThat(refreshedToken).isEqualTo(token); // Same token returned
    }

    @Test
    void givenTokenCloseToExpiration_whenRefreshTokenIfNeeded_thenReturnsNewToken() {
        String token = createTokenCloseToExpiration(testUserId);
        String refreshedToken = jwtService.refreshTokenIfNeeded(token);

        assertThat(refreshedToken).isNotEqualTo(token);
        assertThat(jwtService.extractUserId(refreshedToken)).isEqualTo(testUserId);
        assertThat(jwtService.isTokenValid(refreshedToken)).isTrue();
    }

    @Test
    void givenInvalidToken_whenExtractUserId_thenThrowsException() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> jwtService.extractUserId(invalidToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void givenMalformedToken_whenIsTokenValid_thenThrowException() {
        String malformedToken = "malformed.token.4534534";
        assertThatThrownBy(() -> jwtService.isTokenValid(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void givenDifferentUserIds_whenGenerateToken_thenGeneratesDifferentTokens() {
        UUID userId1 = testUserId;
        UUID userId2 = UUID.randomUUID();

        String token1 = jwtService.generateToken(userId1);
        String token2 = jwtService.generateToken(userId2);

        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.extractUserId(token1)).isEqualTo(userId1);
        assertThat(jwtService.extractUserId(token2)).isEqualTo(userId2);
    }

    @Test
    void givenSameUserId_whenGenerateTokenMultipleTimes_thenGeneratesDifferentTokens() {
        Instant time1 = Instant.parse("2023-01-01T10:00:00Z");
        Instant time2 = time1.plusSeconds(1);

        when(clock.instant()).thenReturn(time1, time2);

        String token1 = jwtService.generateToken(testUserId);
        String token2 = jwtService.generateToken(testUserId);

        assertThat(token1).isNotEqualTo(token2);

        assertThat(jwtService.extractUserId(token1)).isEqualTo(testUserId);
        assertThat(jwtService.extractUserId(token2)).isEqualTo(testUserId);
    }

    @Test
    void givenGeneratedToken_whenParsed_thenContainsCorrectExpiration() {
        String token = jwtService.generateToken(testUserId);

        Claims claims = parseToken(token);
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        long actualExpiration = expiration.getTime() - issuedAt.getTime();
        assertThat(actualExpiration).isCloseTo(TEST_JWT_EXPIRATION, Offset.offset(1000L));
    }

    @Test
    void givenGeneratedToken_whenParsed_thenContainsCorrectSubject() {
        String token = jwtService.generateToken(testUserId);

        Claims claims = parseToken(token);
        String subject = claims.getSubject();

        assertThat(subject).isEqualTo(testUserId.toString());
    }

    private String createExpiredToken(UUID userId) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_KEY));
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(fixedInstant.minusSeconds(7200))) // issued 2 hours ago
                .setExpiration(Date.from(fixedInstant.minusSeconds(3600))) // expired 1 hour ago
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createTokenCloseToExpiration(UUID userId) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_KEY));

        long closeToExpirationTime = 300000; // 5 minutes (less than threshold of 10 minutes)

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(fixedInstant))
                .setExpiration(Date.from(fixedInstant.plusMillis(closeToExpirationTime)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseToken(String token) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_KEY));

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setClock(() -> Date.from(fixedInstant))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
