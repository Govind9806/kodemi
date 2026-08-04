package com.example.user_service.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class FeignRequestInterceptorTest {

    @Test
    void apply_withTraceIdInMdc_addsTraceHeader() {
        FeignRequestInterceptor interceptor = new FeignRequestInterceptor();
        RequestTemplate template = mock(RequestTemplate.class);

        MDC.put("traceId", "trace-999");
        try {
            interceptor.apply(template);
            verify(template).header("X-Trace-Id", "trace-999");
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    void apply_withoutTraceIdInMdc_doesNotAddHeader() {
        FeignRequestInterceptor interceptor = new FeignRequestInterceptor();
        RequestTemplate template = mock(RequestTemplate.class);

        MDC.remove("traceId");
        assertDoesNotThrow(() -> interceptor.apply(template));
        verify(template, never()).header(anyString(), anyString());
    }
}
