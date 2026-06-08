package com.edustudio.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtTokenUtil {

    private final String secret;
    private final long expirationMinutes;
    private final String issuer;

    public JwtTokenUtil(
            @Value("${eduagent.jwt.secret:eduagent-studio-dev-secret-change-in-production}") String secret,
            @Value("${eduagent.jwt.expiration-minutes:1440}") Long expirationMinutes,
            @Value("${eduagent.jwt.issuer:eduagent-studio}") String issuer
    ) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes == null ? 1440L : expirationMinutes;
        this.issuer = issuer;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiresAt = Date.from(now.toInstant().plusSeconds(expirationMinutes * 60));
        return Jwts.builder()
                .issuer(issuer)
                .subject(principal.getUsername())
                .claim("userId", principal.getUserId())
                .claim("username", principal.getUsername())
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Object userId = parseClaims(token).get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(userId));
    }

    public String getUsername(String token) {
        Claims claims = parseClaims(token);
        Object username = claims.get("username");
        return username == null ? claims.getSubject() : String.valueOf(username);
    }

    public LocalDateTime getExpiresAt(String token) {
        return LocalDateTime.ofInstant(parseClaims(token).getExpiration().toInstant(), ZoneId.systemDefault());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
