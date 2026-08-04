package com.example.payment_service.component;

import com.example.payment_service.util.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

@Aspect
@Component
public class RoleCheckAspect {

    @Value("${jwt.secret}")
    private String secretKeyString;
    private final JwtUtil jwtUtil;

    public RoleCheckAspect(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

   @Around("@annotation(requiresRole)")
    public Object checkRole(
            ProceedingJoinPoint joinPoint,
            RequiresRole requiresRole) throws Throwable {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Request context not available");
        }

        String authHeader =
                attributes.getRequest().getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid token");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid token");
        }

        String role = jwtUtil.extractRole(token);

        boolean allowed = false;
        for (String r : requiresRole.value()) {
            if (r.equalsIgnoreCase(role)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied");
        }

        return joinPoint.proceed();
    }
}