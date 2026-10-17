package com.futmanager.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final String authMode;

    public JwtService(
            @Value("${futmanager.auth.supabase.jwt-secret:super-secret-jwt-key-for-futmanager-development-must-be-at-least-256-bits-long}") String jwtSecret,
            @Value("${futmanager.auth.mode:dev}") String authMode) {
        
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            this.secretKey = Keys.hmacShaKeyFor(padded);
        } else {
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
        this.authMode = authMode;
    }

    public Claims validateAndExtractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("Falha na validação do token JWT: {}", e.getMessage());
            return null;
        }
    }

    public String extractEmail(String token) {
        Claims claims = validateAndExtractClaims(token);
        if (claims == null) {
            return null;
        }
        String email = claims.get("email", String.class);
        if (email != null && !email.isBlank()) {
            return email;
        }
        return claims.getSubject();
    }

    public String generateDevToken(UUID userId, String email, String role) {
        return generateDevToken(userId, email, role, null, null);
    }

    public String generateDevToken(UUID userId, String email, String role, UUID tenantId, String tenantSlug) {
        return generateDevToken(userId, email, role, tenantId, tenantSlug, 1);
    }

    public String generateDevToken(UUID userId, String email, String role, UUID tenantId, String tenantSlug, Integer tokenVersion) {
        long expirationMillis = 1000L * 60 * 60 * 24 * 7; // 7 dias
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        var builder = Jwts.builder()
                .subject(userId != null ? userId.toString() : email)
                .claim("email", email)
                .claim("role", role)
                .claim("token_version", tokenVersion != null ? tokenVersion : 1)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer("futmanager-auth");

        if (tenantId != null) {
            builder.claim("tenant_id", tenantId.toString());
        }
        if (tenantSlug != null) {
            builder.claim("tenant_slug", tenantSlug);
        }

        return builder.signWith(secretKey).compact();
    }
}
