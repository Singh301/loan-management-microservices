package com.loanmanagement.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN = "ACCESS";
    private static final String REFRESH_TOKEN = "REFRESH";
    private static final String ROLES_CLAIM = "roles";
    private static final String USER_ID_CLAIM = "userId";
    private static final int MIN_SECRET_LENGTH_BYTES = 32;

    private final String jwtSecret;
    private final long expiration;
    private final long refreshExpiration;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtSecret = jwtProperties.getSecret();
        this.expiration = jwtProperties.getExpiration();
        this.refreshExpiration = jwtProperties.getRefreshExpiration();
    }

    @jakarta.annotation.PostConstruct
    void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, Long userId, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        USER_ID_CLAIM, userId,
                        ROLES_CLAIM, roles,
                        TOKEN_TYPE_CLAIM, ACCESS_TOKEN
                ))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(username)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        return validateTokenType(token, ACCESS_TOKEN);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenType(token, REFRESH_TOKEN);
    }

    private boolean validateTokenType(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            return expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        return parseClaims(token).get(USER_ID_CLAIM, Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        List<String> roles = parseClaims(token).get(ROLES_CLAIM, List.class);
        return roles == null ? List.of() : List.copyOf(roles);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
