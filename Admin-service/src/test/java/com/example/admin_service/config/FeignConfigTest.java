package com.example.admin_service.config;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeignConfigTest {

    private FeignConfig feignConfig;

    @BeforeEach
    void setUp() {
        feignConfig = new FeignConfig();
    }

    @Test
    void requestInterceptorShouldPropagateAuthorizationHeader() {
        // Mock HttpServletRequest
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getHeader("Authorization")).thenReturn("Bearer test-token");

        ServletRequestAttributes mockAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(mockAttributes);

        RequestTemplate template = new RequestTemplate();

        feignConfig.requestInterceptor().apply(template);

        assertEquals("Bearer test-token", template.headers().get("Authorization").iterator().next());

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requestInterceptorShouldNotFailWhenNoRequest() {
        RequestContextHolder.resetRequestAttributes();

        RequestTemplate template = new RequestTemplate();
        feignConfig.requestInterceptor().apply(template);

        assertEquals(0, template.headers().size());
    }

    @Test
    void requestInterceptorShouldNotFailWhenNoAuthorizationHeader() {
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getHeader("Authorization")).thenReturn(null);

        ServletRequestAttributes mockAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(mockAttributes);

        RequestTemplate template = new RequestTemplate();
        feignConfig.requestInterceptor().apply(template);

        assertEquals(0, template.headers().size());

        RequestContextHolder.resetRequestAttributes();
    }
}