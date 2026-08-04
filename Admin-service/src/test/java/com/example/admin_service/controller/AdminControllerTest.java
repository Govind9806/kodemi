package com.example.admin_service.controller;

import com.example.admin_service.dto.request.*;
import com.example.admin_service.dto.response.*;
import com.example.admin_service.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private final String TOKEN = "Bearer token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ===== CREATE SUB ADMIN =====
    @Test
    void subAdmin_success() {
        SubAdminRequest request = new SubAdminRequest();
        when(adminService.subAdminCreate(TOKEN, request)).thenReturn("Created");

        ResponseEntity<String> response = adminController.subAdmin(TOKEN, request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Created", response.getBody());
    }

    // ===== UPDATE SUB ADMIN =====
    @Test
    void setSubAdmin_success() {
        SubAdminDetailsDTO request = new SubAdminDetailsDTO();
        when(adminService.setSubAdmin(request, TOKEN)).thenReturn("Updated");

        ResponseEntity<Object> response = adminController.setSubAdmin(request, TOKEN);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Updated", response.getBody());
    }

    // ===== CHANGE PASSWORD =====
    @Test
    void updatePassword_success() {
        PasswordChange request = new PasswordChange();
        request.setPassword("newPass");
        when(adminService.changePassword("newPass", TOKEN)).thenReturn("Changed");

        ResponseEntity<String> response = adminController.updatePassword(request, TOKEN);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Changed", response.getBody());
    }

    // ===== LOGIN =====
    @Test
    void adminLogin_success() {
        AdminLoginDTO request = new AdminLoginDTO();
        when(adminService.login(request)).thenReturn("token");

        ResponseEntity<Object> response = adminController.adminLogin(request);
        assertEquals("token", response.getBody());
    }

    // ===== GET USER =====
    @Test
    void getTrainer_success() {
        when(adminService.getUser(TOKEN, "1")).thenReturn("data");

        ResponseEntity<Object> response = adminController.getTrainer(TOKEN, "1");
        assertEquals("data", response.getBody());
    }

    // ===== ALL TRAINERS =====
    @Test
    void getAllTrainer() {
        List<TrainerResponseDTO> list = List.of();
        when(adminService.getAllTrainer(TOKEN)).thenReturn(list);

        ResponseEntity<List<TrainerResponseDTO>> response = adminController.getAllTrainer(TOKEN);
        assertEquals(list, response.getBody());
    }

    // ===== PAYOUTS =====
    @Test
    void getAllPayouts() {
        List<PayoutRequest> list = List.of();
        when(adminService.getAllPayouts(TOKEN)).thenReturn(list);

        ResponseEntity<List<PayoutRequest>> response = adminController.getAllPayouts(TOKEN);
        assertEquals(list, response.getBody());
    }

    @Test
    void processPayoutRequest_success() {
        ProcessPayoutRequest request = new ProcessPayoutRequest();
        when(adminService.processPayoutRequest(TOKEN, request)).thenReturn("processed");

        ResponseEntity<String> response = adminController.processPayoutRequest(TOKEN, request);
        assertEquals("processed", response.getBody());
    }

    @Test
    void processPayoutRequestByPath_success() {
        when(adminService.processPayoutRequestByPath(TOKEN, "APPROVE", "p1", "ok")).thenReturn("done");

        ResponseEntity<String> response = adminController.processPayoutRequestByPath("p1", "APPROVE", "ok", TOKEN);
        assertEquals("done", response.getBody());
    }

    @Test
    void getTransactionHistory_success() {
        TransactionHistoryResponse response = new TransactionHistoryResponse();
        when(adminService.getAllTransactionHistory()).thenReturn(response);

        ResponseEntity<TransactionHistoryResponse> res = adminController.getTransactionHistory();
        assertEquals(response, res.getBody());
    }

    // ===== BROADCAST & SUSPEND =====
    @Test
    void broadcastAnnouncement_success() {
        AdminBroadcastRequest request = new AdminBroadcastRequest();
        when(adminService.broadcastAnnouncement(TOKEN, request)).thenReturn("Broadcast Sent");

        ResponseEntity<String> response = adminController.broadcastAnnouncement(TOKEN, request);
        assertEquals("Broadcast Sent", response.getBody());
    }

    @Test
    void suspendUser_success() {
        when(adminService.suspendUser(TOKEN, "u1", "Reason")).thenReturn("Suspended");

        ResponseEntity<String> response = adminController.suspendUser(TOKEN, "u1", "Reason");
        assertEquals("Suspended", response.getBody());
    }

    // ===== COMPAT MODERATIONS =====
    @Test
    void courseReviewCompat_success() {
        Map<String, Object> mockRes = new HashMap<>();
        CourseModerationRequest request = new CourseModerationRequest();
        request.setAction("APPROVE");
        request.setRemarks("remarks");

        when(adminService.courseModeration(TOKEN, "c1", "APPROVE", "remarks")).thenReturn(mockRes);

        ResponseEntity<Map<String, Object>> response = adminController.courseReviewCompat(TOKEN, "c1", request, null, null);
        assertEquals(mockRes, response.getBody());
    }

    @Test
    void trainerReviewCompat_success() {
        Object mockRes = new Object();
        TrainerReviewRequest request = new TrainerReviewRequest();
        request.setAction("APPROVE");
        request.setRemarks("remarks");

        when(adminService.trainerModeration(TOKEN, "t1", "APPROVE", "remarks")).thenReturn(mockRes);

        ResponseEntity<Object> response = adminController.trainerReviewCompat(TOKEN, "t1", request, null, null);
        assertEquals(mockRes, response.getBody());
    }
}