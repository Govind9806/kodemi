package com.example.enrollment_progress_service.controller;

import com.example.enrollment_progress_service.component.RequiresRole;
import com.example.enrollment_progress_service.dto.request.EnrollmentRequest;
import com.example.enrollment_progress_service.dto.request.UnenrollRequest;
import com.example.enrollment_progress_service.dto.request.BatchEnrollmentStatsRequest;
import com.example.enrollment_progress_service.dto.response.AccessCheckResponse;
import com.example.enrollment_progress_service.dto.response.EnrollmentResponse;
import com.example.enrollment_progress_service.dto.response.EnrollmentStatusResponse;
import com.example.enrollment_progress_service.dto.response.CourseEnrollmentStatsResponse;
import com.example.enrollment_progress_service.enums.EnrollmentTargetType;
import com.example.enrollment_progress_service.model.EnrollmentEntity;
import com.example.enrollment_progress_service.service.EnrollmentService;
import com.example.enrollment_progress_service.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final JwtUtil jwtUtil;

    @PostMapping("/enrollments/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody EnrollmentRequest request) {
        String userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(enrollmentService.enroll(userId, request, token));
    }

    @PostMapping("/enrollments/unenroll")
    public ResponseEntity<EnrollmentResponse> unenroll(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UnenrollRequest request) {
        String userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(enrollmentService.unenroll(userId, request));
    }

    @GetMapping("/enrollments/my")
    public ResponseEntity<List<EnrollmentEntity>> getMyEnrollments(
            @RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(userId));
    }

    @GetMapping("/enrollments/status")
    public ResponseEntity<EnrollmentStatusResponse> getStatus(
            @RequestHeader("Authorization") String token,
            @RequestParam("targetId") String targetId,
            @RequestParam("targetType") String targetType) {
        String userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(enrollmentService.getStatus(userId, targetId, targetType));
    }

    @GetMapping("/enrollments/internal/access-check")
    public ResponseEntity<AccessCheckResponse> checkAccess(

            @RequestParam("userId") String userId,
            @RequestParam("targetId") String targetId,
            @RequestParam("targetType") String targetType) {
        log.info("chck hit");
        return ResponseEntity.ok(enrollmentService.checkAccess(userId, targetId, targetType));
    }

    @GetMapping("/enrollments/internal/user/{userId}")
    public ResponseEntity<List<EnrollmentEntity>> getUserEnrollmentsInternal(
            @PathVariable("userId") String userId) {
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(userId));
    }

    // Legacy APIs
    @GetMapping("/enrollment/course/{courseId}/status")
    public ResponseEntity<EnrollmentStatusResponse> getLegacyCourseStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable("courseId") String courseId,
            @RequestParam(value = "targetType", defaultValue = "RECORDED_COURSE") String targetType) {
        String userId = jwtUtil.extractUserId(token);
        EnrollmentStatusResponse status = enrollmentService.getStatus(userId, courseId, targetType);
        // Fallback: if not enrolled with given targetType, try the other course type
        if (!status.isEnrolled()) {
            String fallbackType = "RECORDED_COURSE".equals(targetType) ? "LIVE_COURSE" : "RECORDED_COURSE";
            EnrollmentStatusResponse fallback = enrollmentService.getStatus(userId, courseId, fallbackType);
            if (fallback != null && fallback.isEnrolled()) {
                return ResponseEntity.ok(fallback);
            }
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping({"/enrollments/conference/{conferenceId}/status", "/enrollments/conferenceIdDetails/{conferenceId}"})
    public ResponseEntity<EnrollmentStatusResponse> getLegacyConferenceStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable("conferenceId") String conferenceId) {
        String userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(enrollmentService.getStatus(userId, conferenceId, EnrollmentTargetType.CONFERENCE.name()));
    }

    @GetMapping("/enrollments/internal/course/{courseId}/learners")
    public ResponseEntity<List<String>> getEnrolledLearners(@PathVariable("courseId") String courseId) {
        return ResponseEntity.ok(enrollmentService.getEnrolledLearners(courseId));
    }

    @GetMapping("/enrollments/course/{courseId}/stats")
    public ResponseEntity<CourseEnrollmentStatsResponse> getCourseStats(
            @PathVariable("courseId") String courseId,
            @RequestParam(value = "targetType", defaultValue = "RECORDED_COURSE") String targetType) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentStats(courseId, targetType));
    }

    @PostMapping("/enrollments/courses/stats")
    public ResponseEntity<java.util.Map<String, CourseEnrollmentStatsResponse>> getBatchStats(
            @Valid @RequestBody BatchEnrollmentStatsRequest request) {
        return ResponseEntity.ok(enrollmentService.getBatchEnrollmentStats(request));
    }

    @GetMapping("/enrollments/internal/course/{courseId}/stats")
    public ResponseEntity<CourseEnrollmentStatsResponse> getInternalCourseStats(
            @PathVariable("courseId") String courseId,
            @RequestParam(value = "targetType", defaultValue = "RECORDED_COURSE") String targetType) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentStats(courseId, targetType));
    }

    @PostMapping("/enrollments/internal/courses/stats")
    public ResponseEntity<java.util.Map<String, CourseEnrollmentStatsResponse>> getInternalBatchStats(
            @Valid @RequestBody BatchEnrollmentStatsRequest request) {
        return ResponseEntity.ok(enrollmentService.getBatchEnrollmentStats(request));
    }

    @RequiresRole("TRAINER")
    @GetMapping("/enrollments/my/students")
    public ResponseEntity<List<com.example.enrollment_progress_service.dto.response.TrainerCourseStudentsResponse>> getMyStudent(
            @RequestHeader("Authorization") String token
    ){
        return ResponseEntity.ok(enrollmentService.getMyStudents(token));
    }
}
