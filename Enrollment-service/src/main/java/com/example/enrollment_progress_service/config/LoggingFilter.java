package com.example.enrollment_progress_service.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class LoggingFilter implements Filter {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String TRACE_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpServletRequest) {
            String traceId = httpServletRequest.getHeader(TRACE_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }
            MDC.put(TRACE_KEY, traceId);

            if (response instanceof HttpServletResponse httpServletResponse) {
                httpServletResponse.setHeader(TRACE_HEADER, traceId);
            }

            long startTime = System.currentTimeMillis();
            String method = httpServletRequest.getMethod();
            String uri = httpServletRequest.getRequestURI();
            String query = httpServletRequest.getQueryString();
            String fullPath = query != null ? uri + "?" + query : uri;

            log.info("Incoming request: {} {} [IP: {}]", method, fullPath, httpServletRequest.getRemoteAddr());

            try {
                chain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                int status = 200;
                if (response instanceof HttpServletResponse httpServletResponse) {
                    status = httpServletResponse.getStatus();
                }
                log.info("Outgoing response: {} {} -> HTTP {} (took {}ms)", method, fullPath, status, duration);
                MDC.remove(TRACE_KEY);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
