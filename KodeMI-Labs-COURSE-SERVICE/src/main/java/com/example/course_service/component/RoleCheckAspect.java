package com.example.course_service.component;

import com.example.course_service.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    public Object checkRole(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) throws Throwable {

        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Request context not available");
        }
        
        HttpServletRequest request = requestAttributes.getRequest();

        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
        }

        String role = jwtUtil.extractRole(token);

        if (role != null && Arrays.asList(requiresRole.value()).contains(role)) {
            return joinPoint.proceed();
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }


}