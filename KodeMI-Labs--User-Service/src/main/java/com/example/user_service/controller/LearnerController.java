package com.example.user_service.controller;


import com.example.user_service.commondto.UserDTO;
import com.example.user_service.component.RequiresRole;
import com.example.user_service.dto.request.LearnerRequestDTO;
import com.example.user_service.dto.response.UserNotificationTargetDTO;
import com.example.user_service.service.impl.FileServiceImpl;
import com.example.user_service.exception.UserServiceException;
import com.example.user_service.util.JwtUtil;

import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.exception.LearnerNotFoundException;
import com.example.user_service.service.LearnerService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learner")
@Slf4j
public class LearnerController {

    private static final String INVALID_USER_ID_MSG = "Invalid userId provided: {}";
    private static final String LEARNER_NOT_FOUND_MSG = "Learner not found: {}";
    private static final String SERVICE_FETCH_ERROR_MSG = "Service error while fetching learner: {}";

    private final LearnerService learnerService;
    private final FileServiceImpl fileService;
    private final JwtUtil jwtUtil;

    public LearnerController(LearnerService learnerService,
                             FileServiceImpl fileService, JwtUtil jwtUtil) {
        this.learnerService = learnerService;
        this.fileService = fileService;
        this.jwtUtil = jwtUtil;
    }
    @PostMapping("/create")
    public ResponseEntity<String> createLearner(@Valid @RequestBody UserDTO userDTO) {
        try {
            learnerService.createLearner(userDTO);
            log.info("Learner created successfully for username: {}", userDTO.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Learner Created Successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for learner creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (UserServiceException e) {
            log.error("Service error while creating learner: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create learner");
        }
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LearnerResponseDTO> updateProfile(
            @RequestHeader("Authorization") String token,
            @Valid @RequestPart(value = "body", required = false) LearnerRequestDTO requestDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            String userId = jwtUtil.extractUserId(token);
            LearnerResponseDTO result = learnerService.updateProfile(userId, requestDTO, file);
            log.info("Learner profile updated successfully for userId: {}", userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input for update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (LearnerNotFoundException e) {
            log.warn("Learner not found for update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (UserServiceException e) {
            log.error("Service error while updating learner: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @GetMapping("/detail")
    public ResponseEntity<LearnerResponseDTO> getProfile(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        log.info("Fetching learner details for userId: {}", userId);

        try {
            LearnerResponseDTO result = learnerService.getProfileByUserId(userId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn(INVALID_USER_ID_MSG, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (LearnerNotFoundException e) {
            log.warn(LEARNER_NOT_FOUND_MSG, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (UserServiceException e) {
            log.error(SERVICE_FETCH_ERROR_MSG, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("details/{id}")
    public ResponseEntity<LearnerResponseDTO> getLearner(@PathVariable("id") String id) {
        try {
            LearnerResponseDTO result = learnerService.getProfileByUserId(id);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn(INVALID_USER_ID_MSG, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (LearnerNotFoundException e) {
            log.warn(LEARNER_NOT_FOUND_MSG, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (UserServiceException e) {
            log.error(SERVICE_FETCH_ERROR_MSG, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequiresRole("USER_ADMIN")
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<String> deleteProfile(@PathVariable String userId) {
        try {
            learnerService.deleteProfile(userId);
            log.info("Learner profile deleted successfully for userId: {}", userId);
            return ResponseEntity.ok("Learner profile deleted successfully");

        } catch (IllegalArgumentException e) {
            log.warn(INVALID_USER_ID_MSG, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (LearnerNotFoundException e) {
            log.warn("Learner not found for deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (UserServiceException e) {
            log.error("Service error while deleting learner: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete learner");
        }
    }
    @GetMapping("/profiles")
    public ResponseEntity<byte[]> getObject( @RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        byte[] response = fileService.getObjectAsBytes(userId);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/notification-targets")
    public ResponseEntity<List<UserNotificationTargetDTO>> getLearnerNotificationTargets() {
        List<UserNotificationTargetDTO> targets =
                learnerService.getLearnerNotificationTargets();

        return ResponseEntity.ok(targets);
    }

    @PostMapping("/follow/{trainerId}")
    public ResponseEntity<java.util.Map<String, Object>> followTrainer(
            @RequestHeader("Authorization") String token,
            @PathVariable("trainerId") String trainerId) {
        String userId = jwtUtil.extractUserId(token);
        learnerService.followTrainer(userId, trainerId);
        return ResponseEntity.ok(java.util.Map.of("message", "Successfully followed trainer", "trainerId", trainerId, "followed", true));
    }

    @DeleteMapping("/unfollow/{trainerId}")
    public ResponseEntity<java.util.Map<String, Object>> unfollowTrainer(
            @RequestHeader("Authorization") String token,
            @PathVariable("trainerId") String trainerId) {
        String userId = jwtUtil.extractUserId(token);
        learnerService.unfollowTrainer(userId, trainerId);
        return ResponseEntity.ok(java.util.Map.of("message", "Successfully unfollowed trainer", "trainerId", trainerId, "followed", false));
    }

    @GetMapping("/followed-trainers")
    public ResponseEntity<List<String>> getFollowedTrainers(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        List<String> followedTrainers = learnerService.getFollowedTrainers(userId);
        return ResponseEntity.ok(followedTrainers);
    }
}