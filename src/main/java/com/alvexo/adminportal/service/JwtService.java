package com.alvexo.adminportal.service;

import com.alvexo.adminportal.entity.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HexFormat;

/**
 * Issues/validates this service's own admin access tokens (see jwt.* in
 * application.properties). Access tokens only for now — no refresh-token
 * rotation, since no story yet needs silent renewal.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secretHex,
            @Value("${jwt.access-token.expiration}") long accessTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secretHex));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(AdminUser admin) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);
        return Jwts.builder()
                .subject(String.valueOf(admin.getId()))
                .claim("email", admin.getEmail())
                .claim("fullName", admin.getFullName())
                .claim("role", admin.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    /** Returns parsed claims, or null if the token is missing/expired/invalid. */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
