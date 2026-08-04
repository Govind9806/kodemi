package com.example.enrollment_progress_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class FeignRequestInterceptor implements RequestInterceptor {
    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String TRACE_KEY = "traceId";

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(TRACE_KEY);
        if (traceId != null) {
            template.header(TRACE_HEADER, traceId);
        }
    }
}
