package com.example.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class LoggingFilterTest {

    private LoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoggingFilter();
    }

    @Test
    void doFilter_httpServletRequestResponse_withTraceHeader() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Trace-Id")).thenReturn("test-trace-123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/learners/u1");
        when(request.getQueryString()).thenReturn("param=value");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);

        filter.doFilter(request, response, chain);

        verify(response).setHeader("X-Trace-Id", "test-trace-123");
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_httpServletRequestResponse_withoutTraceHeader() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/trainers");
        when(request.getQueryString()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(eq("X-Trace-Id"), anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_nonHttpRequestResponse_callsChain() throws ServletException, IOException {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
        verify(chain).doFilter(request, response);
    }
}
