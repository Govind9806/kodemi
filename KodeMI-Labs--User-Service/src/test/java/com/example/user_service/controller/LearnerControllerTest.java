package com.example.user_service.controller;

import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.exception.LearnerNotFoundException;
import com.example.user_service.exception.UserServiceException;
import com.example.user_service.service.LearnerService;
import com.example.user_service.service.impl.FileServiceImpl;
import com.example.user_service.util.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class LearnerControllerTest {

    @Mock
    private LearnerService learnerService;

    @Mock
    private FileServiceImpl fileService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private LearnerController learnerController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ===== getProfile =====
    @Test
    void getProfile_success() {
        String token = "Bearer token";
        String userId = "user123";
        LearnerResponseDTO dto = new LearnerResponseDTO();

        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(learnerService.getProfileByUserId(userId)).thenReturn(dto);

        ResponseEntity<LearnerResponseDTO> response = learnerController.getProfile(token);

        assertEquals(dto, response.getBody());
    }

    @Test
    void getProfile_badRequest() {
        String token = "token";

        when(jwtUtil.extractUserId(token)).thenReturn("invalid");
        when(learnerService.getProfileByUserId("invalid"))
                .thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.getProfile(token);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getProfile_notFound() {
        String token = "token";

        when(jwtUtil.extractUserId(token)).thenReturn("user123");
        when(learnerService.getProfileByUserId("user123"))
                .thenThrow(new LearnerNotFoundException("User profile not found for userId: Not found"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.getProfile(token);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getProfile_internalError() {
        String token = "token";

        when(jwtUtil.extractUserId(token)).thenReturn("user123");
        when(learnerService.getProfileByUserId("user123"))
                .thenThrow(new UserServiceException("Error"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.getProfile(token);

        assertEquals(500, response.getStatusCode().value());
    }

    // ===== getLearner =====
    @Test
    void getLearner_success() {
        LearnerResponseDTO dto = new LearnerResponseDTO();

        when(learnerService.getProfileByUserId("user123")).thenReturn(dto);

        ResponseEntity<LearnerResponseDTO> response = learnerController.getLearner("user123");

        assertEquals(dto, response.getBody());
    }

    @Test
    void getLearner_badRequest() {
        when(learnerService.getProfileByUserId("invalid"))
                .thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.getLearner("invalid");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getLearner_notFound() {
        when(learnerService.getProfileByUserId("user123"))
                .thenThrow(new LearnerNotFoundException("User profile not found for userId: Not found"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.getLearner("user123");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getLearner_internalError() {
        when(learnerService.getProfileByUserId("user123"))
                .thenThrow(new UserServiceException("Error"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.getLearner("user123");

        assertEquals(500, response.getStatusCode().value());
    }

    // ===== deleteProfile =====
    @Test
    void deleteProfile_success() {
        doNothing().when(learnerService).deleteProfile("user123");

        ResponseEntity<String> response = learnerController.deleteProfile("user123");

        assertEquals("Learner profile deleted successfully", response.getBody());
    }

    @Test
    void deleteProfile_badRequest() {
        doThrow(new IllegalArgumentException("Invalid"))
                .when(learnerService).deleteProfile("invalid");

        ResponseEntity<String> response = learnerController.deleteProfile("invalid");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid", response.getBody());
    }

    @Test
    void deleteProfile_notFound() {
        doThrow(new LearnerNotFoundException("User profile not found for userId: Not found"))
                .when(learnerService).deleteProfile("user123");

        ResponseEntity<String> response = learnerController.deleteProfile("user123");

        assertEquals(404, response.getStatusCode().value());
        assertTrue(response.getBody().contains("User profile not found"));
    }

    @Test
    void deleteProfile_internalError() {
        doThrow(new UserServiceException("Error"))
                .when(learnerService).deleteProfile("user123");

        ResponseEntity<String> response = learnerController.deleteProfile("user123");

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Failed to delete learner", response.getBody());
    }

    // ===== getObject =====
    @Test
    void getObject_success() {
        String token = "token";
        String userId = "user123";
        byte[] data = "file".getBytes();

        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(fileService.getObjectAsBytes(userId)).thenReturn(data);

        ResponseEntity<byte[]> response = learnerController.getObject(token);

        assertArrayEquals(data, response.getBody());
    }
}