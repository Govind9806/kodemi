package com.example.course_service.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class FeignConfigTest {

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requestInterceptor_WithAuthHeader_ForwardsToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        FeignConfig config = new FeignConfig();
        RequestTemplate template = new RequestTemplate();
        config.requestInterceptor().apply(template);

        assertTrue(template.headers().containsKey("Authorization"));
        assertEquals("Bearer test-token", template.headers().get("Authorization").iterator().next());
    }

    @Test
    void requestInterceptor_NoAuthHeader_DoesNotForward() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        FeignConfig config = new FeignConfig();
        RequestTemplate template = new RequestTemplate();
        config.requestInterceptor().apply(template);

        assertFalse(template.headers().containsKey("Authorization"));
    }

    @Test
    void requestInterceptor_NullRequestAttributes_DoesNotThrow() {
        RequestContextHolder.resetRequestAttributes();

        FeignConfig config = new FeignConfig();
        RequestTemplate template = new RequestTemplate();

        assertDoesNotThrow(() -> config.requestInterceptor().apply(template));
    }
}
