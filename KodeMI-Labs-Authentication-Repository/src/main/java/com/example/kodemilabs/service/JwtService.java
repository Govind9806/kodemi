package com.example.kodemilabs.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKeyString;

    private Key getKey() {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 256 bits (32 bytes)"
            );
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId, String email, String role, String username) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role",  role);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(Duration.ofDays(7))))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateServiceToken(String serviceName) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "SERVICE");
        claims.put("role", "ROLE_INTERNAL");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(serviceName)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(Duration.ofMinutes(10))))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isServiceToken(String token) {
        String type = extractAllClaims(token).get("type", String.class);
        return "SERVICE".equalsIgnoreCase(type);
    }

    public String getTokenType(String token) {
        String type = extractAllClaims(token).get("type", String.class);
        return (type != null) ? type : "USER";
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(Date.from(Instant.now()));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}