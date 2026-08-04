package com.example.user_service.controller;

import com.example.user_service.commondto.UserDTO;
import com.example.user_service.dto.request.LearnerRequestDTO;
import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.exception.LearnerNotFoundException;
import com.example.user_service.exception.UserServiceException;
import com.example.user_service.service.LearnerService;
import com.example.user_service.service.impl.FileServiceImpl;
import com.example.user_service.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class LearnerControllerExtendedTest {

    @InjectMocks
    private LearnerController learnerController;

    @Mock
    private LearnerService learnerService;

    @Mock
    private FileServiceImpl fileService;

    @Mock
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ── createLearner ────────────────────────────────────────────────────

    @Test
    void createLearner_success() {
        UserDTO dto = new UserDTO();
        dto.setUserId("u1");
        dto.setUsername("user1");
        doNothing().when(learnerService).createLearner(dto);

        ResponseEntity<String> response = learnerController.createLearner(dto);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Learner Created Successfully", response.getBody());
    }

    @Test
    void createLearner_illegalArgument_returns400() {
        UserDTO dto = new UserDTO();
        doThrow(new IllegalArgumentException("Invalid input"))
                .when(learnerService).createLearner(dto);

        ResponseEntity<String> response = learnerController.createLearner(dto);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid input", response.getBody());
    }

    @Test
    void createLearner_serviceException_returns500() {
        UserDTO dto = new UserDTO();
        doThrow(new UserServiceException("Service error"))
                .when(learnerService).createLearner(dto);

        ResponseEntity<String> response = learnerController.createLearner(dto);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Failed to create learner", response.getBody());
    }

    // ── updateProfile ────────────────────────────────────────────────────

    @Test
    void updateProfile_success() {
        String token = "Bearer token";
        LearnerRequestDTO requestDTO = new LearnerRequestDTO();
        LearnerResponseDTO responseDTO = new LearnerResponseDTO();
        MockMultipartFile file = new MockMultipartFile("file", "pic.jpg", "image/jpeg", "data".getBytes());

        when(jwtUtil.extractUserId(token)).thenReturn("user123");
        when(learnerService.updateProfile("user123", requestDTO, file)).thenReturn(responseDTO);

        ResponseEntity<LearnerResponseDTO> response = learnerController.updateProfile(token, requestDTO, file);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
    }

    @Test
    void updateProfile_illegalArgument_returns400() {
        String token = "Bearer token";
        LearnerRequestDTO requestDTO = new LearnerRequestDTO();

        when(jwtUtil.extractUserId(token)).thenReturn("user123");
        when(learnerService.updateProfile(eq("user123"), eq(requestDTO), any()))
                .thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.updateProfile(token, requestDTO, null);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void updateProfile_notFound_returns404() {
        String token = "Bearer token";
        LearnerRequestDTO requestDTO = new LearnerRequestDTO();

        when(jwtUtil.extractUserId(token)).thenReturn("user123");
        when(learnerService.updateProfile(eq("user123"), eq(requestDTO), any()))
                .thenThrow(new LearnerNotFoundException("Not found"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.updateProfile(token, requestDTO, null);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateProfile_jwtException_returns401() {
        String token = "Bearer invalid";
        LearnerRequestDTO requestDTO = new LearnerRequestDTO();

        when(jwtUtil.extractUserId(token)).thenReturn("user123");
        when(learnerService.updateProfile(eq("user123"), eq(requestDTO), any()))
                .thenThrow(new JwtException("Invalid JWT"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.updateProfile(token, requestDTO, null);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void updateProfile_serviceException_returns500() {
        String token = "Bearer token";
        LearnerRequestDTO requestDTO = new LearnerRequestDTO();

        when(jwtUtil.extractUserId(token)).thenReturn("user123");
        when(learnerService.updateProfile(eq("user123"), eq(requestDTO), any()))
                .thenThrow(new UserServiceException("Service error"));

        ResponseEntity<LearnerResponseDTO> response = learnerController.updateProfile(token, requestDTO, null);

        assertEquals(500, response.getStatusCode().value());
    }

    // ── follow / unfollow / followed-trainers ────────────────────────────

    @Test
    void followTrainer_success() {
        String token = "Bearer token";
        when(jwtUtil.extractUserId(token)).thenReturn("u1");
        doNothing().when(learnerService).followTrainer("u1", "t1");

        ResponseEntity<java.util.Map<String, Object>> response = learnerController.followTrainer(token, "t1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Successfully followed trainer", response.getBody().get("message"));
    }

    @Test
    void unfollowTrainer_success() {
        String token = "Bearer token";
        when(jwtUtil.extractUserId(token)).thenReturn("u1");
        doNothing().when(learnerService).unfollowTrainer("u1", "t1");

        ResponseEntity<java.util.Map<String, Object>> response = learnerController.unfollowTrainer(token, "t1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Successfully unfollowed trainer", response.getBody().get("message"));
    }

    @Test
    void getFollowedTrainers_success() {
        String token = "Bearer token";
        when(jwtUtil.extractUserId(token)).thenReturn("u1");
        when(learnerService.getFollowedTrainers("u1")).thenReturn(java.util.List.of("t1", "t2"));

        ResponseEntity<java.util.List<String>> response = learnerController.getFollowedTrainers(token);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getLearnerNotificationTargets_success() {
        when(learnerService.getLearnerNotificationTargets()).thenReturn(java.util.Collections.emptyList());

        ResponseEntity<java.util.List<com.example.user_service.dto.response.UserNotificationTargetDTO>> response =
                learnerController.getLearnerNotificationTargets();

        assertEquals(200, response.getStatusCode().value());
    }
}
