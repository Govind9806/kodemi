package com.example.user_service.component;

import com.example.user_service.exception.NoActiveRequestException;
import com.example.user_service.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class RoleCheckAspect {

    private final JwtUtil jwtUtil;

    public RoleCheckAspect(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) throws Throwable {

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (!(requestAttributes instanceof ServletRequestAttributes attributes)) {
            throw new NoActiveRequestException(
                    "RequestAttributes is null or not an instance of ServletRequestAttributes. " +
                            "This usually happens outside an HTTP request thread."
            );
        }

        HttpServletRequest request = attributes.getRequest();

        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }

        try {
            String role = jwtUtil.extractRole(token);
            if (role == null) {
                log.warn("Role not found in token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Role not found in token");
            }
            
            if (Arrays.asList(requiresRole.value()).contains(role)) {
                return joinPoint.proceed();
            }
            
            log.warn("Access denied for role: {}", role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            
        } catch (JwtException e) {
            log.warn("Invalid or expired token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        } catch (Exception e) {
            log.error("Error validating token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token validation failed");
        }
    }

}