package com.example.kodemilabs.feign;

import com.example.kodemilabs.service.JwtService;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceTokenInterceptorTest {

    private JwtService jwtService;
    private ServiceTokenInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        interceptor = new ServiceTokenInterceptor(jwtService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void apply_authorizationHeaderPresent_shouldNotOverride() {
        RequestTemplate template = new RequestTemplate();
        template.header("Authorization", "Bearer existingToken");

        interceptor.apply(template);

        verifyNoInteractions(jwtService);
        assertEquals("Bearer existingToken", template.headers().get("Authorization").iterator().next());
    }

    @Test
    void apply_noAuthHeader_noSecurityContext_shouldAttachServiceToken() {
        RequestTemplate template = new RequestTemplate();
        when(jwtService.generateServiceToken("auth-service")).thenReturn("mockServiceToken");

        interceptor.apply(template);

        verify(jwtService).generateServiceToken("auth-service");
        assertTrue(template.headers().containsKey("Authorization"));
        assertEquals("Bearer mockServiceToken", template.headers().get("Authorization").iterator().next());
    }

    @Test
    void apply_withUserDetailsSecurityContext_shouldAttachServiceToken() {
        RequestTemplate template = new RequestTemplate();
        when(jwtService.generateServiceToken("auth-service")).thenReturn("mockServiceToken");

        UserDetails userDetails = mock(UserDetails.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        interceptor.apply(template);

        verify(jwtService).generateServiceToken("auth-service");
        assertEquals("Bearer mockServiceToken", template.headers().get("Authorization").iterator().next());
    }
}
