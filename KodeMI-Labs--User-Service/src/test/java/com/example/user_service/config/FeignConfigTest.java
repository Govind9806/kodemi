package com.example.user_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Mockito.*;

class FeignConfigTest {

    private FeignConfig feignConfig;

    @BeforeEach
    void setUp() {
        feignConfig = new FeignConfig();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requestInterceptor_withAuthHeader_addsAuthHeaderToTemplate() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = mock(RequestTemplate.class);

        interceptor.apply(template);

        verify(template).header("Authorization", "Bearer token123");
    }

    @Test
    void requestInterceptor_withoutAuthHeader_doesNotAddHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = mock(RequestTemplate.class);

        interceptor.apply(template);

        verify(template, never()).header(anyString(), anyString());
    }

    @Test
    void requestInterceptor_nullAttributes_doesNotThrow() {
        RequestContextHolder.resetRequestAttributes();
        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = mock(RequestTemplate.class);

        interceptor.apply(template);

        verify(template, never()).header(anyString(), anyString());
    }
}
