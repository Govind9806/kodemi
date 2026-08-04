package com.example.user_service.component;


import com.example.user_service.exception.NoActiveRequestException;
import com.example.user_service.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleCheckAspectTest {

    @InjectMocks
    private RoleCheckAspect roleCheckAspect;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private RequiresRole requiresRole;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletRequestAttributes attributes;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ✅ 1. No Request Context
    @Test
    void shouldThrowException_whenNoRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        assertThrows(NoActiveRequestException.class, () ->
                roleCheckAspect.checkRole(joinPoint, requiresRole)
        );
    }

    // ✅ 2. Missing Token → 401
    @Test
    void shouldReturn401_whenTokenMissing() throws Throwable {
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getHeader("Authorization")).thenReturn(null);

        Object result = roleCheckAspect.checkRole(joinPoint, requiresRole);

        ResponseEntity<?> response = (ResponseEntity<?>) result;

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Missing or invalid token", response.getBody());

        verify(joinPoint, never()).proceed();
    }

    // ✅ 3. Invalid Token Format → 401
    @Test
    void shouldReturn401_whenInvalidTokenFormat() throws Throwable {
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getHeader("Authorization")).thenReturn("InvalidToken");

        Object result = roleCheckAspect.checkRole(joinPoint, requiresRole);

        ResponseEntity<?> response = (ResponseEntity<?>) result;

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Missing or invalid token", response.getBody());

        verify(joinPoint, never()).proceed();
    }

    // ✅ 4. Role is null → 401
    @Test
    void shouldReturn401_whenRoleIsNull() throws Throwable {
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractRole("Bearer token")).thenReturn(null);

        Object result = roleCheckAspect.checkRole(joinPoint, requiresRole);

        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(401, response.getStatusCode().value());

        verify(jwtUtil).extractRole("Bearer token");
    }

    // ✅ 5. Role NOT allowed → 403
    @Test
    void shouldReturn403_whenRoleNotAllowed() throws Throwable {
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractRole("Bearer token")).thenReturn("LEARNER");

        when(requiresRole.value()).thenReturn(new String[]{"SUPER_ADMIN"});

        Object result = roleCheckAspect.checkRole(joinPoint, requiresRole);

        ResponseEntity<?> response = (ResponseEntity<?>) result;

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Access denied", response.getBody());

        verify(joinPoint, never()).proceed();
    }

    // ✅ 6. Multiple Roles → Allowed
    @Test
    void shouldProceed_whenRoleInMultipleAllowedRoles() throws Throwable {
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractRole("Bearer token")).thenReturn("ADMIN");

        when(requiresRole.value()).thenReturn(new String[]{"ADMIN", "SUPER_ADMIN"});
        when(joinPoint.proceed()).thenReturn("SUCCESS");

        Object result = roleCheckAspect.checkRole(joinPoint, requiresRole);

        assertEquals("SUCCESS", result);
        verify(joinPoint).proceed();
    }

    // ✅ 7. Role allowed → proceed()
    @Test
    void shouldProceed_whenRoleAllowed() throws Throwable {
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractRole("Bearer token")).thenReturn("SUPER_ADMIN");

        when(requiresRole.value()).thenReturn(new String[]{"SUPER_ADMIN"});
        when(joinPoint.proceed()).thenReturn("SUCCESS");

        Object result = roleCheckAspect.checkRole(joinPoint, requiresRole);

        assertEquals("SUCCESS", result);
        verify(joinPoint).proceed();
    }
}