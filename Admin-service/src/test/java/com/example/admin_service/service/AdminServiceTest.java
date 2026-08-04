package com.example.admin_service.service;

import com.example.admin_service.dto.request.*;
import com.example.admin_service.dto.response.*;
import com.example.admin_service.enums.AdminRole;
import com.example.admin_service.enums.Role;
import com.example.admin_service.exceptions.*;
import com.example.admin_service.feign.*;
import com.example.admin_service.model.Admin;
import com.example.admin_service.model.AdminRateLimit;
import com.example.admin_service.repository.AdminRateLimitRepository;
import com.example.admin_service.util.JwtUtil;
import com.example.admin_service.service.notification.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    private AdminService             adminService;
    private UserClient               userClient;
    private AuthClient               authClient;
    private CourseClient             courseClient;
    private PaymentClient            paymentClient;
    private AdminRepository          adminRepository;
    private JwtUtil                  jwtUtil;
    private PasswordEncoder          passwordEncoder;
    private SecureRandom             secureRandom;
    private EmailService             emailService;
    private NotificationPublisher   notificationPublisher;
    private AdminRateLimitRepository adminRateLimitRepository;

    private static final String TOKEN = "Bearer token";

    @BeforeEach
    void setup() {
        userClient               = mock(UserClient.class);
        authClient               = mock(AuthClient.class);
        courseClient             = mock(CourseClient.class);
        paymentClient            = mock(PaymentClient.class);
        adminRepository          = mock(AdminRepository.class);
        jwtUtil                  = mock(JwtUtil.class);
        passwordEncoder          = mock(PasswordEncoder.class);
        secureRandom             = mock(SecureRandom.class);
        emailService             = mock(EmailService.class);
        notificationPublisher    = mock(NotificationPublisher.class);
        adminRateLimitRepository = mock(AdminRateLimitRepository.class);

        // Mock secureRandom behaviour so generatePassword() doesn't throw null pointer exceptions
        when(secureRandom.nextInt(anyInt())).thenReturn(0);

        adminService = new AdminService(
                userClient, authClient, courseClient, paymentClient,
                adminRepository, jwtUtil, passwordEncoder, secureRandom,
                emailService, notificationPublisher, adminRateLimitRepository
        );
    }

    private Admin buildAdmin(boolean pending) {
        Admin admin = new Admin();
        admin.setAdminId("a1");
        admin.setPassword("hashed");
        admin.setPending(pending);
        admin.setEmail("admin@test.com");
        admin.setAdminRole(AdminRole.USER_ADMIN);
        return admin;
    }

    // ── getUser ──────────────────────────────────────────────────────────

    @Test
    void getUser_trainerRole_returnsTrainer() {
        UserDTO dto = mock(UserDTO.class);
        when(dto.getRole()).thenReturn(Role.TRAINER);
        TrainerResponseDTO response = new TrainerResponseDTO();
        when(authClient.geUserById(TOKEN, "1")).thenReturn(dto);
        when(userClient.getTrainerById(TOKEN, "1")).thenReturn(response);

        assertSame(response, adminService.getUser(TOKEN, "1"));
        verify(userClient).getTrainerById(TOKEN, "1");
    }

    @Test
    void getUser_superAdminRole_returnsAdmin() {
        UserDTO dto = mock(UserDTO.class);
        when(dto.getRole()).thenReturn(Role.SUPER_ADMIN);
        AdminResponseDTO response = new AdminResponseDTO();
        when(authClient.geUserById(TOKEN, "1")).thenReturn(dto);
        when(userClient.getAdmin("1")).thenReturn(response);

        assertSame(response, adminService.getUser(TOKEN, "1"));
        verify(userClient).getAdmin("1");
    }

    @Test
    void getUser_learnerRole_returnsLearner() {
        UserDTO dto = mock(UserDTO.class);
        when(dto.getRole()).thenReturn(Role.LEARNER);
        LearnerResponseDTO response = new LearnerResponseDTO();
        when(authClient.geUserById(TOKEN, "1")).thenReturn(dto);
        when(userClient.getLearner(TOKEN, "1")).thenReturn(response);

        assertSame(response, adminService.getUser(TOKEN, "1"));
        verify(userClient).getLearner(TOKEN, "1");
    }

    // ── Trainer actions ───────────────────────────────────────────────────

    @Test
    void trainerModeration_validId_callsAuthClient() {
        Object response = new Object();
        when(authClient.reviewTrainer(eq(TOKEN), eq("t1"), any(TrainerReviewRequest.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(response));

        Object result = adminService.trainerModeration(TOKEN, "t1", "APPROVE", "Ok");
        assertNotNull(result);
        verify(authClient).reviewTrainer(eq(TOKEN), eq("t1"), any(TrainerReviewRequest.class));
    }

    @Test
    void trainerModeration_nullId_returnsPendingTrainers() {
        List<TrainerResponseDTO> pending = new ArrayList<>();
        when(userClient.getAllPendingTrainers(TOKEN)).thenReturn(pending);

        Object result = adminService.trainerModeration(TOKEN, null, "APPROVE", "Ok");
        assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(pending, map.get("data"));
    }

    @Test
    void getAllTrainer_returnsListFromUserClient() {
        List<TrainerResponseDTO> list = Collections.emptyList();
        when(userClient.getAllTrainers(TOKEN)).thenReturn(list);
        assertEquals(list, adminService.getAllTrainer(TOKEN));
    }

    // ── Course actions ────────────────────────────────────────────────────

    @Test
    void courseModeration_validId_callsCourseClient() {
        Map<String, Object> mockRes = new HashMap<>();
        when(courseClient.reviewCourse(eq(TOKEN), eq("c1"), any(CourseModerationRequest.class)))
                .thenReturn(mockRes);

        Map<String, Object> result = adminService.courseModeration(TOKEN, "c1", "APPROVE", "Good");
        assertEquals(mockRes, result);
        verify(courseClient).reviewCourse(eq(TOKEN), eq("c1"), any(CourseModerationRequest.class));
    }

    @Test
    void courseModeration_nullId_returnsPendingCourses() {
        CourseResponseDTO pendingCourse = new CourseResponseDTO();
        pendingCourse.setStatus("PENDING");
        CourseResponseDTO activeCourse = new CourseResponseDTO();
        activeCourse.setStatus("ACTIVE");

        when(courseClient.getAllCoursesAdmin(TOKEN)).thenReturn(List.of(pendingCourse, activeCourse));

        Map<String, Object> result = adminService.courseModeration(TOKEN, null, null, null);
        assertNotNull(result);
        List<?> data = (List<?>) result.get("data");
        assertEquals(1, data.size());
        assertEquals("PENDING", ((CourseResponseDTO) data.get(0)).getStatus());
    }

    // ── login ─────────────────────────────────────────────────────────────

    @Test
    void login_success_notPending_returnsToken() {
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setEmail("admin@test.com");
        dto.setPassword("pass");

        Admin admin = buildAdmin(false);
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(admin);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(authClient.adminLogin(any())).thenReturn("JWT_TOKEN");

        assertEquals("JWT_TOKEN", adminService.login(dto));
    }

    @Test
    void login_pendingAdmin_returnsResponseWrapper() {
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setEmail("admin@test.com");
        dto.setPassword("pass");

        Admin admin = buildAdmin(true);
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(admin);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(authClient.adminLogin(any())).thenReturn("JWT_TOKEN");

        Object result = adminService.login(dto);

        assertInstanceOf(Response.class, result);
        Response<?> response = (Response<?>) result;
        assertFalse(response.isSuccess());
        assertEquals("Admin Pending.", response.getMessage());
    }

    @Test
    void login_invalidPassword_throwsInvalidCredentialsException() {
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setEmail("admin@test.com");
        dto.setPassword("wrong");

        Admin admin = buildAdmin(false);
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(admin);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> adminService.login(dto));
        verify(authClient, never()).adminLogin(any());
    }

    @Test
    void login_adminNotFound_throwsAdminNotFoundException() {
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setEmail("admin@test.com");
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(null);

        assertThrows(AdminNotFoundException.class,
                () -> adminService.login(dto));
    }

    // ── subAdminCreate ────────────────────────────────────────────────────

    @Test
    void subAdminCreate_success_savesAndReturnsMessage() {
        try (var mocked = mockStatic(PasswordValidator.class)) {
            mocked.when(() -> PasswordValidator.validate(any())).thenAnswer(i -> null);

            SubAdminRequest request = new SubAdminRequest();
            request.setEmail("sub1@test.com");
            request.setUsername("sub1");
            request.setAdminRole(AdminRole.USER_ADMIN);

            String result = adminService.subAdminCreate(TOKEN, request);

            assertTrue(result.contains("Sub Admin Created"));
            assertTrue(result.contains("USER_ADMIN"));
            verify(adminRepository).save(any(Admin.class));
            verify(emailService).sendOEmail(eq("sub1@test.com"), anyString(), eq("sub1"));
        }
    }

    // ── setSubAdmin ───────────────────────────────────────────────────────

    @Test
    void setSubAdmin_pendingAdmin_updatesAndReturnsSuccess() {
        try (var mocked = mockStatic(PasswordValidator.class)) {
            mocked.when(() -> PasswordValidator.validate(any())).thenAnswer(i -> null);

            SubAdminDetailsDTO dto = new SubAdminDetailsDTO();
            dto.setUsername("newUser");
            dto.setPassword("Password123!");

            Admin admin = buildAdmin(true);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn("a1");
            when(adminRepository.findById("a1")).thenReturn(admin);

            String result = adminService.setSubAdmin(dto, TOKEN);

            assertEquals("Username Set.", result);
            assertFalse(admin.isPending());
            assertEquals("newUser", admin.getUsername());
            verify(adminRepository).save(admin);
        }
    }

    @Test
    void setSubAdmin_alreadyUpdated_returnsAlreadyUpdatedMessage() {
        SubAdminDetailsDTO dto = new SubAdminDetailsDTO();
        Admin admin = buildAdmin(false);

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("a1");
        when(adminRepository.findById("a1")).thenReturn(admin);

        String result = adminService.setSubAdmin(dto, TOKEN);

        assertEquals("SubAdmin Already Updated.", result);
        verify(adminRepository, never()).save(any());
    }

    // ── changePassword ────────────────────────────────────────────────────

    @Test
    void changePassword_success_updatesPassword() {
        try (var mocked = mockStatic(PasswordValidator.class)) {
            mocked.when(() -> PasswordValidator.validate(any())).thenAnswer(i -> null);

            Admin admin = buildAdmin(false);
            when(jwtUtil.extractRole(TOKEN)).thenReturn("SUPER_ADMIN");
            when(adminRepository.findByRole(AdminRole.SUPER_ADMIN)).thenReturn(admin);

            String result = adminService.changePassword("Password123!", TOKEN);

            assertEquals("Password Changed Successfully.", result);
            verify(adminRepository).save(admin);
        }
    }

    // ── payouts & transactions ─────────────────────────────────────────────

    @Test
    void getAllPayouts_success() {
        List<PayoutRequest> list = List.of(new PayoutRequest());
        when(paymentClient.getAllPayouts(TOKEN)).thenReturn(list);

        List<PayoutRequest> result = adminService.getAllPayouts(TOKEN);
        assertEquals(list, result);
    }

    @Test
    void getAllPayouts_fails_throwsFetchPendingPayoutException() {
        when(paymentClient.getAllPayouts(TOKEN)).thenThrow(new RuntimeException("Error"));
        assertThrows(FetchPendingPayoutException.class, () -> adminService.getAllPayouts(TOKEN));
    }

    @Test
    void getAllTransactionHistory_success() {
        TransactionHistoryResponse response = new TransactionHistoryResponse();
        when(paymentClient.getTransactionHistory()).thenReturn(response);

        assertEquals(response, adminService.getAllTransactionHistory());
    }

    @Test
    void getAllTransactionHistory_fails_throwsFetchPendingPayoutException() {
        when(paymentClient.getTransactionHistory()).thenThrow(new RuntimeException("Error"));
        assertThrows(FetchPendingPayoutException.class, () -> adminService.getAllTransactionHistory());
    }

    @Test
    void processPayoutRequest_success() {
        ProcessPayoutRequest request = new ProcessPayoutRequest();
        when(paymentClient.processPayoutRequest(request, TOKEN)).thenReturn("Success");

        assertEquals("Success", adminService.processPayoutRequest(TOKEN, request));
    }

    @Test
    void processPayoutRequest_nullToken_throws() {
        assertThrows(UnauthorizedPayoutAccessException.class, () -> adminService.processPayoutRequest(null, new ProcessPayoutRequest()));
    }

    @Test
    void processPayoutRequestByPath_success() {
        when(paymentClient.processPayoutRequestByPath(TOKEN, "APPROVE", "p1", "ok")).thenReturn("Success");
        assertEquals("Success", adminService.processPayoutRequestByPath(TOKEN, "APPROVE", "p1", "ok"));
    }

    @Test
    void processPayoutRequestByPath_invalidAction_throws() {
        assertThrows(InvalidPayoutActionException.class, () -> adminService.processPayoutRequestByPath(TOKEN, "CANCEL", "p1", "ok"));
    }

    // ── announcements & suspension ─────────────────────────────────────────

    @Test
    void broadcastAnnouncement_success() {
        AdminBroadcastRequest request = new AdminBroadcastRequest();
        request.setTitle("Announcement");
        request.setMessage("Message");
        request.setTargetRole("ALL");
        request.setUrgent(true);
        request.setChannels(new ArrayList<>(List.of("EMAIL", "SMS")));

        when(jwtUtil.extractRole(TOKEN)).thenReturn("SUPER_ADMIN");
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin-1");
        when(adminRateLimitRepository.getAdminRateLimit(anyString())).thenReturn(null);

        String result = adminService.broadcastAnnouncement(TOKEN, request);
        assertEquals("Broadcast sent successfully", result);
        verify(notificationClient).broadcastNotification(eq(TOKEN), any());
        verify(adminRateLimitRepository).save(any());
    }

    @Test
    void broadcastAnnouncement_rateLimitExceeded_throws() {
        AdminBroadcastRequest request = new AdminBroadcastRequest();
        AdminRateLimit rateLimit = new AdminRateLimit();
        rateLimit.setBroadcastCount(5);

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin-1");
        when(adminRateLimitRepository.getAdminRateLimit(anyString())).thenReturn(rateLimit);

        assertThrows(RuntimeException.class, () -> adminService.broadcastAnnouncement(TOKEN, request));
    }

    @Test
    void suspendUser_success() {
        String result = adminService.suspendUser(TOKEN, "user-1", "Spam");
        assertEquals("User suspended and notified successfully", result);
        verify(notificationClient).sendInternalNotification(eq(TOKEN), any());
    }
}