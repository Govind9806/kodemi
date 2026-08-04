package com.example.admin_service.component;

import com.example.admin_service.exceptions.NoActiveRequestException;
import com.example.admin_service.util.JwtUtil;
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
        }
        String role = jwtUtil.extractRole(token);;
        if (role == null) {
            throw new NullPointerException("Value Not Found.");
        }
        if (Arrays.asList(requiresRole.value()).contains(role)) {
            return joinPoint.proceed();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }

}