package com.example.user_service.controller;

import com.example.user_service.component.RequiresRole;
import com.example.user_service.dto.request.TrainerRequestDTO;
import com.example.user_service.dto.response.TrainerAdminResponse;
import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.dto.response.TrainerUserResponseDTO;
import com.example.user_service.dto.update.TrainerUpdateRequestDTO;
import com.example.user_service.service.TrainerService;
import com.example.user_service.service.impl.FileServiceImpl;
import com.example.user_service.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Set;


@RestController
@Slf4j
@RequestMapping("/api/v1/trainer")
public class TrainerController {

    private final TrainerService service;
    private final FileServiceImpl fileService;
    private final JwtUtil  jwtUtil;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public TrainerController(TrainerService service, FileServiceImpl fileService, JwtUtil jwtUtil, ObjectMapper objectMapper, Validator validator) {
        this.service = service;
        this.fileService = fileService;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @RequiresRole("TRAINER")
    @PostMapping("/apply")
    public ResponseEntity<String> createTrainer(
            @RequestPart("body") String requestStr,
            @RequestPart("demo") MultipartFile demo,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestPart(value = "globalCertifications", required = false) MultipartFile globalCertifications,
            @RequestPart(value = "totRegistration", required = false) MultipartFile totRegistration,
            @RequestPart(value = "supportingDocuments", required = false) MultipartFile supportingDocuments,
            @RequestHeader("Authorization") String token) {
        log.info("hitt apply with payload: {}", requestStr);

        String userId = jwtUtil.extractUserId(token);

        TrainerRequestDTO request;
        try {
            request = objectMapper.readValue(requestStr, TrainerRequestDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse TrainerRequestDTO JSON", e);
            return ResponseEntity.badRequest().body("Invalid JSON payload for trainer application");
        }

        if (request == null) {
            return ResponseEntity.badRequest().body("TrainerRequestDTO cannot be null");
        }

        Set<ConstraintViolation<TrainerRequestDTO>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<TrainerRequestDTO> violation : violations) {
                sb.append(violation.getMessage()).append("; ");
            }
            return ResponseEntity.badRequest().body("Validation failed: " + sb.toString());
        }

        com.example.user_service.dto.request.TrainerProfileFiles files = new com.example.user_service.dto.request.TrainerProfileFiles(
                demo, profileImage, globalCertifications, totRegistration, supportingDocuments);

        String result = service.createTrainerProfile(request, files, userId, token);

        return ResponseEntity.ok(result);
    }

    // TO get Profile Details
    @GetMapping("/detail")
    public ResponseEntity<TrainerResponseDTO> getTrainer(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(service.getTrainerProfileById(userId));
    }

    //for Feign Client
    @GetMapping("/details/{id}")
    public ResponseEntity<TrainerResponseDTO> getTrainerById(@PathVariable("id") String trainerId){
        return ResponseEntity.ok(service.getTrainerProfileById(trainerId));
    }

    @PatchMapping("/update")
    public ResponseEntity<String> updateTrainer(
            @RequestHeader("Authorization") String token,
            @RequestPart(value = "body", required = false) String requestStr,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        String userId = jwtUtil.extractUserId(token);
        log.info("Hit update with payload: {}", requestStr);

        TrainerUpdateRequestDTO request = null;
        if (requestStr != null && !requestStr.trim().isEmpty()) {
            try {
                request = objectMapper.readValue(requestStr, TrainerUpdateRequestDTO.class);
            } catch (Exception e) {
                log.error("Failed to parse TrainerUpdateRequestDTO JSON", e);
                return ResponseEntity.badRequest().body("Invalid JSON payload for trainer update");
            }
        }

        if (request == null) {
            request = new TrainerUpdateRequestDTO();
        }

        // Normalize empty phone number to null to avoid pattern validation failure
        if (request.getPhoneNumber() != null && request.getPhoneNumber().trim().isEmpty()) {
            request.setPhoneNumber(null);
        }

        Set<ConstraintViolation<TrainerUpdateRequestDTO>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<TrainerUpdateRequestDTO> violation : violations) {
                sb.append(violation.getMessage()).append("; ");
            }
            return ResponseEntity.badRequest().body("Validation failed: " + sb.toString());
        }

        return ResponseEntity.ok(service.updateTrainerProfile(userId, request, file));
    }

    @RequiresRole("USER_ADMIN")
    @GetMapping({"/detail/all", "/admin/all"})
    public ResponseEntity<List<TrainerResponseDTO>> getAllTrainers(
            @RequestHeader("Authorization")  String token
    ) {
        log.info("Admin fetching all trainers");
        return ResponseEntity.ok(service.getAllTrainers());
    }


    @GetMapping("/admin/details/all")
    public ResponseEntity<List<TrainerAdminResponse>> getAllTrainersForAdmin(
            @RequestHeader("Authorization") String token
    ){
        return ResponseEntity.ok(service.getAllTrainersForAdmin());
    }

    @RequiresRole("USER_ADMIN")
    @GetMapping("/all/pending")
    public ResponseEntity<List<TrainerResponseDTO>> getAllPendingTrainers(@RequestHeader("Authorization")  String token){
        return ResponseEntity.ok(service.getAllPendingTrainers());
    }

    @GetMapping("/all")
    public ResponseEntity<List<TrainerUserResponseDTO>> getAllTrainersForUsers() {
        log.info("Fetching all trainers for users");
        return ResponseEntity.ok(service.getAllTrainersForUsers());
    }

    @GetMapping("/video")
    public ResponseEntity<String> getTrainerVideo(@RequestParam String key) {
        return ResponseEntity.ok(fileService.generatePresignedUrl(key));
    }

    @PostMapping("/internal/evict-cache")
    @org.springframework.cache.annotation.CacheEvict(value = {"allTrainers", "allPendingTrainers", "allTrainersForUsers"}, allEntries = true)
    public ResponseEntity<String> evictTrainerCaches() {
        log.info("Evicting trainer caches");
        return ResponseEntity.ok("Caches evicted");
    }
}
