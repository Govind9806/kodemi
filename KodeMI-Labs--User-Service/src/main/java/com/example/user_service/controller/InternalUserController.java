package com.example.user_service.controller;

import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.dto.response.UserNotificationTargetDTO;
import com.example.user_service.service.LearnerService;
import com.example.user_service.service.TrainerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/user/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final LearnerService learnerService;
    private final TrainerService trainerService;

    @Value("${internal.service.key:default-secret}")
    private String internalServiceKey;

    @GetMapping("/contact/{userId}")
    public ResponseEntity<UserNotificationTargetDTO> getUserContactDetails(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String serviceKey,
            @PathVariable String userId) {
        
        if (serviceKey == null || !internalServiceKey.equals(serviceKey)) {
            log.warn("Unauthorized access attempt to internal contact API for user {}", userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Fetching internal user contact details for userId: {}", userId);

        try {
            // First check learner
            LearnerResponseDTO learner = learnerService.getProfileByUserId(userId);
            if (learner != null) {
                return ResponseEntity.ok(UserNotificationTargetDTO.builder()
                        .userId(userId)
                        .email(learner.getEmail())
                        .phoneNumber(learner.getPhoneNumber())
                        .build());
            }
        } catch (Exception e) {
            // Not a learner or not found
            log.debug("UserId {} is not a learner or error occurred: {}", userId, e.getMessage());
        }

        try {
            // Next check trainer
            TrainerResponseDTO trainer = trainerService.getTrainerProfileById(userId);
            if (trainer != null) {
                return ResponseEntity.ok(UserNotificationTargetDTO.builder()
                        .userId(userId)
                        .email(trainer.getEmailId())
                        .phoneNumber(trainer.getPhoneNumber())
                        .build());
            }
        } catch (Exception e) {
            log.debug("UserId {} is not a trainer or error occurred: {}", userId, e.getMessage());
        }

        // Return empty or null if neither
        log.warn("User contact not found for userId: {}", userId);
        return ResponseEntity.notFound().build();
    }
}
