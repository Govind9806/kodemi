package com.example.course_service.controller;

import com.example.course_service.component.RequiresRole;
import com.example.course_service.dto.request.AttachRecordingRequest;
import com.example.course_service.dto.request.LinkLiveSessionRequest;
import com.example.course_service.dto.response.LiveCourseDetailResponseDTO;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.service.LiveCourseService;
import com.example.course_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/course")
public class LiveCourseController {

    private static final String MESSAGE = "message";
    private final LiveCourseService liveCourseService;
    private final EnrollmentClient enrollmentClient;
    private final JwtUtil jwtUtil;

    @RequiresRole("TRAINER")
    @PostMapping("/{courseId}/live-session")
    public ResponseEntity<Map<String, String>> linkLiveSession(
            @RequestHeader("Authorization") String token,
            @PathVariable String courseId,
            @RequestBody LinkLiveSessionRequest request) {
        return ResponseEntity.ok(
            Map.of(MESSAGE, liveCourseService.linkLiveSession(courseId, request))
        );
    }


    @RequiresRole("ADMIN")
    @PostMapping("/internal/attach-recording")
    public ResponseEntity<Map<String, String>> attachRecording(
            @RequestBody AttachRecordingRequest request) {
        return ResponseEntity.ok(
            Map.of(MESSAGE, liveCourseService.attachRecording(request))
        );
    }


    @GetMapping("/live-detail/{courseId}")
    public ResponseEntity<LiveCourseDetailResponseDTO> getLiveCourseDetail(
            @PathVariable String courseId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        LiveCourseDetailResponseDTO liveCourse = liveCourseService.getLiveCourseDetail(courseId);
        
        if (liveCourse != null) {
            if (token != null) {
                return applyEnrollmentMask(liveCourse, courseId, token);
            } else {
                liveCourse.maskVideoContent(); // No token = not enrolled
            }
        }
        
        return ResponseEntity.ok(liveCourse);
    }

    private ResponseEntity<LiveCourseDetailResponseDTO> applyEnrollmentMask(
            LiveCourseDetailResponseDTO liveCourse, String courseId, String token) {
        try {
            String userId = jwtUtil.extractUserId(token);
            boolean isEnrolled = checkEnrollmentSafe(userId, courseId, liveCourse);
            if (!isEnrolled) {
                liveCourse.maskVideoContent();
            }
            return ResponseEntity.ok(liveCourse);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(null); // invalid token
        }
    }

    private boolean checkEnrollmentSafe(String userId, String courseId, LiveCourseDetailResponseDTO liveCourse) {
        try {
            com.example.course_service.dto.response.AccessCheckResponse accessCheck = enrollmentClient.checkAccess(userId, courseId, "LIVE_COURSE");
            return accessCheck != null && accessCheck.isHasAccess();
        } catch (Exception e) {
            liveCourse.maskVideoContent(); // Fail-safe
            return true; // already masked, treat as handled
        }
    }
}

