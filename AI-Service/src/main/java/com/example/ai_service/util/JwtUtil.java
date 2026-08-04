package com.example.ai_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    String secretKeyString;

    Key getKey() {
        return Keys.hmacShaKeyFor(
                secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    private Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new com.example.ai_service.exceptions.UnAuthorizedRequestException("Authorization token is missing");
        }
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(cleanToken)
                    .getBody();
        } catch (Exception e) {
            throw new com.example.ai_service.exceptions.UnAuthorizedRequestException("Invalid or expired token: " + e.getMessage());
        }
    }

    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.isBlank()) {
            userId = claims.getSubject();
        }
        return userId;
    }

    public String extractName(String token) {
        return extractAllClaims(token).get("name", String.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
}