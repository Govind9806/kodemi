package com.example.kodemilabs.feign;

import com.example.kodemilabs.service.JwtService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceTokenInterceptor implements RequestInterceptor {

    private static final String SERVICE_NAME = "auth-service";

    private final JwtService jwtService;

    @Override
    public void apply(RequestTemplate template) {
        // If an Authorization header is already present (e.g. manually set in Feign method), don't override it.
        if (template.headers().containsKey("Authorization")) {
            log.trace("Authorization header already present for Feign call: {}", template.url());
            return;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // 1. If we have a user context, forward the user token
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            // Note: In your architecture, you might need a way to get the original token string 
            // from the security context. If the original token is not stored, fall back to service token or a specific implementation.
            // For now, if no token is available, we fall back to service token.
            log.debug("No raw token available in SecurityContext, generating SERVICE token for Feign call: {}", template.url());
            attachServiceToken(template);
        } else {
            // 2. Otherwise, attach a service-to-service Internal token
            attachServiceToken(template);
        }
    }

    private void attachServiceToken(RequestTemplate template) {
        String serviceToken = jwtService.generateServiceToken(SERVICE_NAME);
        template.header("Authorization", "Bearer " + serviceToken);
        log.debug("Attached SERVICE JWT for Feign call: {}", template.url());
    }
}