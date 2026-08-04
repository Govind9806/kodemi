package com.example.user_service.controller;

import com.example.user_service.dto.request.TrainerRequestDTO;
import com.example.user_service.dto.response.TrainerAdminResponse;
import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.dto.response.TrainerUserResponseDTO;
import com.example.user_service.dto.update.TrainerUpdateRequestDTO;
import com.example.user_service.service.TrainerService;
import com.example.user_service.service.impl.FileServiceImpl;
import com.example.user_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class TrainerControllerTest {

    @InjectMocks
    private TrainerController controller;

    @Mock
    private TrainerService trainerService;

    @Mock
    private FileServiceImpl fileService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private jakarta.validation.Validator validator;

    @InjectMocks
    private TrainerController trainerController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createTrainer_returnsOk() throws Exception {
        String token = "Bearer token";
        String userId = "user123";
        String requestStr = "{}";
        TrainerRequestDTO request = new TrainerRequestDTO();
        MultipartFile demo = mock(MultipartFile.class);
        MultipartFile profileImage = mock(MultipartFile.class);
        MultipartFile globalCertifications = mock(MultipartFile.class);
        MultipartFile totRegistration = mock(MultipartFile.class);
        MultipartFile supportingDocuments = mock(MultipartFile.class);

        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(objectMapper.readValue(requestStr, TrainerRequestDTO.class)).thenReturn(request);
        when(validator.validate(request)).thenReturn(java.util.Collections.emptySet());
        when(trainerService.createTrainerProfile(eq(request), any(com.example.user_service.dto.request.TrainerProfileFiles.class), eq(userId), eq(token))).thenReturn("Created");

        ResponseEntity<String> response = trainerController.createTrainer(
                requestStr, demo, profileImage, globalCertifications, totRegistration, supportingDocuments, token);

        assertEquals("Created", response.getBody());
        verify(trainerService, times(1)).createTrainerProfile(eq(request), any(com.example.user_service.dto.request.TrainerProfileFiles.class), eq(userId), eq(token));
    }

    @Test
    void testGetTrainer() {
        String token = "Bearer test-token";
        TrainerResponseDTO dto = new TrainerResponseDTO();
        when(jwtUtil.extractUserId(token)).thenReturn("user123");
        when(trainerService.getTrainerProfileById("user123")).thenReturn(dto);

        ResponseEntity<TrainerResponseDTO> response = controller.getTrainer(token);

        assertEquals(dto, response.getBody());
    }

    @Test
    void testGetTrainerById() {
        TrainerResponseDTO dto = new TrainerResponseDTO();
        when(trainerService.getTrainerProfileById("trainer123")).thenReturn(dto);

        ResponseEntity<TrainerResponseDTO> response = controller.getTrainerById("trainer123");

        assertEquals(dto, response.getBody());
    }

    @Test
    void updateTrainer_returnsOk() throws Exception {
        String token = "Bearer token";
        String userId = "user123";
        String requestStr = "{}";
        TrainerUpdateRequestDTO request = new TrainerUpdateRequestDTO();
        MultipartFile file = mock(MultipartFile.class);

        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(objectMapper.readValue(requestStr, TrainerUpdateRequestDTO.class)).thenReturn(request);
        when(validator.validate(request)).thenReturn(java.util.Collections.emptySet());
        when(trainerService.updateTrainerProfile(userId, request, file)).thenReturn("Updated");

        ResponseEntity<String> response = trainerController.updateTrainer(token, requestStr, file);

        assertEquals("Updated", response.getBody());
    }

    @Test
    void testGetAllTrainers() {
        String token = "Bearer test-token";
        List<TrainerResponseDTO> list = Arrays.asList(new TrainerResponseDTO());
        when(trainerService.getAllTrainers()).thenReturn(list);

        ResponseEntity<List<TrainerResponseDTO>> response = controller.getAllTrainers(token);

        assertEquals(list, response.getBody());
    }

    @Test
    void testGetAllTrainersForAdmin() {
        String token = "Bearer test-token";
        List<TrainerAdminResponse> list = Arrays.asList(new TrainerAdminResponse());
        when(trainerService.getAllTrainersForAdmin()).thenReturn(list);

        ResponseEntity<List<TrainerAdminResponse>> response = controller.getAllTrainersForAdmin(token);

        assertEquals(list, response.getBody());
    }

    // ===== getAllTrainersForUsers =====
    @Test
    void getAllTrainersForUsers_returnsList() {
        List<TrainerUserResponseDTO> list = List.of(new TrainerUserResponseDTO());

        when(trainerService.getAllTrainersForUsers()).thenReturn(list);

        ResponseEntity<List<TrainerUserResponseDTO>> response = trainerController.getAllTrainersForUsers();

        assertEquals(list, response.getBody());
    }

    // ===== getTrainerVideo =====
    @Test
    void testGetAllPendingTrainers() {
        String token = "Bearer test-token";
        List<TrainerResponseDTO> list = Arrays.asList(new TrainerResponseDTO());
        when(trainerService.getAllPendingTrainers()).thenReturn(list);

        ResponseEntity<List<TrainerResponseDTO>> response = controller.getAllPendingTrainers(token);

        assertEquals(list, response.getBody());
    }

    @Test
    void testGetTrainerVideo() {
        String key = "demoKey123";
        when(fileService.generatePresignedUrl(key)).thenReturn("url123");

        ResponseEntity<String> response = controller.getTrainerVideo(key);

        assertEquals("url123", response.getBody());
    }
}