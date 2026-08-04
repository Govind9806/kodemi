package com.example.course_service.component;

import com.example.course_service.util.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleCheckAspectTest {

    private RoleCheckAspect aspect;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setup() {
        jwtUtil = mock(JwtUtil.class);
        aspect = new RoleCheckAspect(jwtUtil);
        ReflectionTestUtils.setField(aspect, "secretKeyString", "test-secret");
    }

    private void setRequestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("Authorization", token);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void checkRole_ValidRole_ProceedsWithJoinPoint() throws Throwable {
        setRequestWithToken("Bearer valid-token");
        when(jwtUtil.extractRole("Bearer valid-token")).thenReturn("TRAINER");

        RequiresRole requiresRole = mock(RequiresRole.class);
        when(requiresRole.value()).thenReturn(new String[]{"TRAINER"});

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.checkRole(joinPoint, requiresRole);

        assertEquals("success", result);
        verify(joinPoint).proceed();
    }

    @Test
    void checkRole_WrongRole_ReturnsForbidden() throws Throwable {
        setRequestWithToken("Bearer valid-token");
        when(jwtUtil.extractRole("Bearer valid-token")).thenReturn("STUDENT");

        RequiresRole requiresRole = mock(RequiresRole.class);
        when(requiresRole.value()).thenReturn(new String[]{"TRAINER"});

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        Object result = aspect.checkRole(joinPoint, requiresRole);

        assertInstanceOf(ResponseEntity.class, result);
        assertEquals(403, ((ResponseEntity<?>) result).getStatusCode().value());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void checkRole_MissingToken_ReturnsUnauthorized() throws Throwable {
        setRequestWithToken(null);

        RequiresRole requiresRole = mock(RequiresRole.class);
        when(requiresRole.value()).thenReturn(new String[]{"TRAINER"});

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        Object result = aspect.checkRole(joinPoint, requiresRole);

        assertInstanceOf(ResponseEntity.class, result);
        assertEquals(401, ((ResponseEntity<?>) result).getStatusCode().value());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void checkRole_NullRequestAttributes_ReturnsInternalServerError() throws Throwable {
        RequestContextHolder.resetRequestAttributes(); // simulate null context

        RequiresRole requiresRole = mock(RequiresRole.class);
        when(requiresRole.value()).thenReturn(new String[]{"TRAINER"});

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        Object result = aspect.checkRole(joinPoint, requiresRole);

        assertInstanceOf(ResponseEntity.class, result);
        assertEquals(500, ((ResponseEntity<?>) result).getStatusCode().value());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void checkRole_TokenWithoutBearerPrefix_ReturnsUnauthorized() throws Throwable {
        setRequestWithToken("InvalidToken");

        RequiresRole requiresRole = mock(RequiresRole.class);
        when(requiresRole.value()).thenReturn(new String[]{"TRAINER"});

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        Object result = aspect.checkRole(joinPoint, requiresRole);

        assertInstanceOf(ResponseEntity.class, result);
        assertEquals(401, ((ResponseEntity<?>) result).getStatusCode().value());
    }

    @Test
    void checkRole_NullRole_ReturnsForbidden() throws Throwable {
        setRequestWithToken("Bearer valid-token");
        when(jwtUtil.extractRole("Bearer valid-token")).thenReturn(null);

        RequiresRole requiresRole = mock(RequiresRole.class);
        when(requiresRole.value()).thenReturn(new String[]{"TRAINER"});

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        Object result = aspect.checkRole(joinPoint, requiresRole);

        assertInstanceOf(ResponseEntity.class, result);
        assertEquals(403, ((ResponseEntity<?>) result).getStatusCode().value());
    }

    @Test
    void checkRole_AdminRole_ProceedsWithJoinPoint() throws Throwable {
        setRequestWithToken("Bearer admin-token");
        when(jwtUtil.extractRole("Bearer admin-token")).thenReturn("ADMIN");

        RequiresRole requiresRole = mock(RequiresRole.class);
        when(requiresRole.value()).thenReturn(new String[]{"ADMIN"});

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("admin-result");

        Object result = aspect.checkRole(joinPoint, requiresRole);

        assertEquals("admin-result", result);
    }
}