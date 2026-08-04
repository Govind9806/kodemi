package com.example.admin_service.component;

import com.example.admin_service.exceptions.NoActiveRequestException;
import com.example.admin_service.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        assertEquals("Missing token", response.getBody());

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
        assertEquals("Missing token", response.getBody());

        verify(joinPoint, never()).proceed();
    }

    // ✅ 4. Role is null → Exception
    @Test
    void shouldThrowException_whenRoleIsNull() {
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractRole("Bearer token")).thenReturn(null);

        assertThrows(NullPointerException.class, () ->
                roleCheckAspect.checkRole(joinPoint, requiresRole)
        );

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