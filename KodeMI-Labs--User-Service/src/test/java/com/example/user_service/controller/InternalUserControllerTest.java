package com.example.user_service.controller;

import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.dto.response.UserNotificationTargetDTO;
import com.example.user_service.service.LearnerService;
import com.example.user_service.service.TrainerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class InternalUserControllerTest {

    @Mock
    private LearnerService learnerService;

    @Mock
    private TrainerService trainerService;

    @InjectMocks
    private InternalUserController internalUserController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(internalUserController, "internalServiceKey", "secret123");
    }

    @Test
    void getUserContactDetails_unauthorized() {
        ResponseEntity<UserNotificationTargetDTO> response = internalUserController.getUserContactDetails("wrongKey", "u1");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        response = internalUserController.getUserContactDetails(null, "u1");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getUserContactDetails_learnerFound() {
        LearnerResponseDTO learner = new LearnerResponseDTO();
        learner.setEmail("learner@mail.com");
        learner.setPhoneNumber("1234567890");

        when(learnerService.getProfileByUserId("u1")).thenReturn(learner);

        ResponseEntity<UserNotificationTargetDTO> response = internalUserController.getUserContactDetails("secret123", "u1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("learner@mail.com", response.getBody().getEmail());
    }

    @Test
    void getUserContactDetails_trainerFound() {
        when(learnerService.getProfileByUserId("t1")).thenThrow(new RuntimeException("Not learner"));

        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setEmailId("trainer@mail.com");
        trainer.setPhoneNumber("0987654321");

        when(trainerService.getTrainerProfileById("t1")).thenReturn(trainer);

        ResponseEntity<UserNotificationTargetDTO> response = internalUserController.getUserContactDetails("secret123", "t1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("trainer@mail.com", response.getBody().getEmail());
    }

    @Test
    void getUserContactDetails_notFound() {
        when(learnerService.getProfileByUserId("x1")).thenReturn(null);
        when(trainerService.getTrainerProfileById("x1")).thenThrow(new RuntimeException("Not trainer"));

        ResponseEntity<UserNotificationTargetDTO> response = internalUserController.getUserContactDetails("secret123", "x1");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
