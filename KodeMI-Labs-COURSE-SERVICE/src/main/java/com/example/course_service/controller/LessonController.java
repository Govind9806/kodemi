package com.example.course_service.controller;

import com.example.course_service.component.RequiresRole;
import com.example.course_service.dto.request.AbortMultipartUploadRequestDTO;
import com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO;
import com.example.course_service.dto.request.MultipartUploadInitRequest;
import com.example.course_service.dto.request.SaveUploadedContentRequest;
import com.example.course_service.dto.request.LiveRecordingRequest;
import com.example.course_service.dto.response.AccessCheckResponse;
import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.LessonResponseDTO;
import com.example.course_service.dto.response.MessageResponseDTO;
import com.example.course_service.dto.response.MultipartUploadInitResponse;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.service.CourseService;
import com.example.course_service.service.LessonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.util.JwtUtil;

@Slf4j
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/lesson")
public class LessonController {

    private final LessonService lessonService;
    private final EnrollmentClient enrollmentClient;
    private final JwtUtil jwtUtil;
    private final CourseService courseService;

    // Error response key constants
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";

    // Role constants (also used as compile-time constants in annotations)
    private static final String ROLE_TRAINER = "TRAINER";
    private static final String ROLE_COURSE_ADMIN = "COURSE_ADMIN";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final String COURSE_TYPE_RECORDED = "RECORDED_COURSE";

    @RequiresRole(ROLE_TRAINER)
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createLesson(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid LessonEntity request) {
        log.info("[API] POST /create - title: {}, videoKey: {}", request.getTitle(), request.getVideoKey());
        return ResponseEntity.ok(lessonService.createLesson(request, token));
    }

    @RequiresRole(ROLE_TRAINER)
    @PutMapping("/edit/{lessonId}")
    public ResponseEntity<MessageResponseDTO> updateLesson(
            @RequestHeader("Authorization") String token,
            @PathVariable String lessonId,
            @RequestBody @Valid LessonEntity request) {

        return ResponseEntity.ok(new MessageResponseDTO(lessonService.updateLesson(token, lessonId, request)));
    }

    @RequiresRole(ROLE_TRAINER)
    @PostMapping("/upload-url")
    public ResponseEntity<Map<String, String>> getUploadUrl(
            @RequestHeader("Authorization") String token,
            @RequestParam String lessonId,
            @RequestParam String fileName) {

        return ResponseEntity.ok(lessonService.generateUploadUrl(token, lessonId, fileName));
    }

    @RequiresRole(ROLE_TRAINER)
    @PostMapping("/multipart/init")
    public ResponseEntity<MultipartUploadInitResponse> initiateMultipartUpload(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid MultipartUploadInitRequest request) {
        log.info("[API] POST /multipart/init - fileName: {}, fileSize: {}", request.getFileName(), request.getFileSize());
        MultipartUploadInitResponse response = lessonService.initiateMultipartUpload(token, request);
        return ResponseEntity.ok(response);
    }

    @RequiresRole(ROLE_TRAINER)
    @GetMapping("/multipart/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrlForPart(
            @RequestHeader("Authorization") String token,
            @RequestParam String uploadId,
            @RequestParam(value = "fileKey", required = false) String fileKey,
            @RequestParam int partNumber) {
        log.info("[API] GET /multipart/presigned-url - uploadId: {}, partNumber: {}", uploadId, partNumber);
        String url = lessonService.generatePartUploadUrl(token, uploadId, fileKey, partNumber);
        return ResponseEntity.ok(Map.of("presignedUrl", url));
    }

    @RequiresRole(ROLE_TRAINER)
    @PostMapping("/multipart/complete")
    public ResponseEntity<CompleteMultipartUploadResponse> completeMultipartUpload(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid CompleteMultipartUploadRequestDTO request) {
        log.info("[API] POST /multipart/complete - uploadId: {}", request.getUploadId());
        CompleteMultipartUploadResponse response = lessonService.completeMultipartUpload(token, request);
        return ResponseEntity.ok(response);
    }

    @RequiresRole(ROLE_TRAINER)
    @PostMapping("/multipart/abort")
    public ResponseEntity<MessageResponseDTO> abortMultipartUpload(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid AbortMultipartUploadRequestDTO request) {

        return ResponseEntity.ok(new MessageResponseDTO(lessonService.abortMultipartUpload(token, request)));
    }

    @RequiresRole(ROLE_TRAINER)
    @PostMapping("/save-content")
    public ResponseEntity<MessageResponseDTO> saveContent(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid SaveUploadedContentRequest request) {

        return ResponseEntity.ok(new MessageResponseDTO(lessonService.saveUploadedContent(token, request)));
    }

    @RequiresRole(ROLE_TRAINER)
    @PostMapping("/live-recording")
    public ResponseEntity<MessageResponseDTO> saveLiveRecording(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid LiveRecordingRequest request) {

        return ResponseEntity.ok(new MessageResponseDTO(lessonService.saveLiveRecording(request)));
    }

    @GetMapping("/get/{lessonId}")
    public ResponseEntity<LessonResponseDTO> getLessonById(
            @PathVariable String lessonId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        LessonResponseDTO lesson = lessonService.getLessonById(lessonId, token);
        String courseId = lesson.getCourseId();

        if (token == null) {
            lesson.maskContent(); // no token -> mask by default
            return ResponseEntity.ok(lesson);
        }

        try {
            String userId = jwtUtil.extractUserId(token);
            if (!hasContentAccess(userId, courseId, token)) {
                lesson.maskContent();
            }
        } catch (Exception e) {
            log.warn("Failed to validate token for lessonId={}", lessonId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LessonResponseDTO());
        }

        return ResponseEntity.ok(lesson);
    }

    @GetMapping("/download-url")
    public ResponseEntity<Object> getDownloadUrl(
            @RequestHeader("Authorization") String token,
            @RequestParam String lessonId,
            @RequestParam(value = "fileKey", required = false) String fileKey) {

        String userId;
        try {
            userId = jwtUtil.extractUserId(token);
        } catch (Exception e) {
            log.warn("Failed to validate token for lessonId={}", lessonId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(ERROR_KEY, "Unauthorized", MESSAGE_KEY, "Invalid or missing token"));
        }

        LessonResponseDTO lesson = lessonService.getLessonById(lessonId, token);
        String courseId = lesson.getCourseId();

        if (!hasContentAccess(userId, courseId, token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(ERROR_KEY, "You must be enrolled to download this content"));
        }

        String resolvedKey = resolveFileKey(lesson, fileKey);
        if (resolvedKey == null || resolvedKey.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Bad Request", MESSAGE_KEY, "No video/file key found for this lesson"));
        }

        String downloadUrl = lessonService.getDownloadUrl(token, lessonId, resolvedKey);
        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }

    private String resolveFileKey(LessonResponseDTO lesson, String fileKey) {
        if (fileKey != null && !fileKey.isBlank()) {
            return fileKey;
        }
        String videoKey = lesson.getVideoKey();
        if (videoKey != null && !videoKey.isBlank()) {
            return videoKey;
        }
        if (lesson.getContentKey() != null && !lesson.getContentKey().isEmpty()) {
            return lesson.getContentKey().get(0).getKey();
        }
        return null;
    }

    @GetMapping("/by-module/{moduleId}")
    public ResponseEntity<List<LessonResponseDTO>> getLessonsByModule(
            @PathVariable String moduleId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        List<LessonResponseDTO> lessons = lessonService.getLessonsByModule(moduleId, token);

        if (lessons.isEmpty()) {
            return ResponseEntity.ok(lessons);
        }

        String courseId = lessons.get(0).getCourseId();

        if (token == null) {
            lessons.forEach(LessonResponseDTO::maskContent);
            return ResponseEntity.ok(lessons);
        }

        try {
            String userId = jwtUtil.extractUserId(token);
            if (!hasContentAccess(userId, courseId, token)) {
                lessons.forEach(LessonResponseDTO::maskContent);
            }
        } catch (Exception e) {
            log.warn("Failed to validate token for moduleId={}", moduleId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(lessons);
    }

    /**
     * Determines whether the given user has full (unmasked) access to content
     * belonging to the given course: either elevated role/ownership access,
     * or an active enrollment.
     */
    private boolean hasContentAccess(String userId, String courseId, String token) {
        return hasElevatedAccess(userId, courseId, token) || isEnrolled(userId, courseId);
    }

    private boolean hasElevatedAccess(String userId, String courseId, String token) {
        try {
            String role = jwtUtil.extractRole(token);
            if (ROLE_COURSE_ADMIN.equals(role) || ROLE_ADMIN.equals(role)) {
                return true;
            }
            CourseResponseDTO course = courseService.getCourseDetail(courseId);
            return course != null && userId.equals(course.getCreatorId());
        } catch (Exception e) {
            log.warn("Failed to check role or creator for courseId={}", courseId, e);
            return false;
        }
    }

    private boolean isEnrolled(String userId, String courseId) {
        try {
            AccessCheckResponse accessCheck = enrollmentClient.checkAccess(userId, courseId, COURSE_TYPE_RECORDED);
            return accessCheck != null && accessCheck.isHasAccess();
        } catch (Exception e) {
            log.warn("Enrollment check failed for courseId={}", courseId, e);
            return false; // fail-safe: treat as not enrolled
        }
    }
}