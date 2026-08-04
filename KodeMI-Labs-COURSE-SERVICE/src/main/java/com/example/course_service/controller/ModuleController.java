package com.example.course_service.controller;

import com.example.course_service.component.RequiresRole;
import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.LessonResponseDTO;
import com.example.course_service.dto.response.ModuleResponseDTO;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.model.ModuleEntity;
import com.example.course_service.service.ModuleService;
import com.example.course_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/module")
public class ModuleController {

    private static final String MESSAGE = "message";
    private final ModuleService moduleService;
    private final JwtUtil jwtUtil;
    private final EnrollmentClient enrollmentClient;
    private final com.example.course_service.service.CourseService courseService;

    @RequiresRole("TRAINER")
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createModule(
            @RequestHeader("Authorization") String token,
            @RequestBody ModuleEntity request) {
        String userId = jwtUtil.extractUserId(token);

        return ResponseEntity.ok(moduleService.createModule(request, userId));
    }

    @RequiresRole("TRAINER")
    @PutMapping("/edit/{moduleId}")
    public ResponseEntity<Map<String, String>> updateModule(
            @RequestHeader("Authorization") String token,
            @PathVariable String moduleId,
            @RequestBody ModuleEntity request) {

        return ResponseEntity.ok(
                Map.of(MESSAGE,
                        moduleService.updateModule(token, moduleId, request))
        );
    }

    @GetMapping("/by-course/{courseId}")
    public ResponseEntity<List<ModuleResponseDTO>> getModulesByCourse(
            @PathVariable String courseId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        List<ModuleResponseDTO> modules = moduleService.getModulesByCourse(courseId);

        if (token != null) {
            return handleAuthenticatedRequest(modules, courseId, token);
        } else {
            // No token provided = mask all content
            maskAllModuleLessons(modules);
            return ResponseEntity.ok(modules);
        }
    }

    private ResponseEntity<List<ModuleResponseDTO>> handleAuthenticatedRequest(
            List<ModuleResponseDTO> modules, String courseId, String token) {
        try {
            String userId = jwtUtil.extractUserId(token);

            if (hasAdminOrCreatorAccess(token, userId, courseId)) {
                return ResponseEntity.ok(modules);
            }

            boolean isEnrolled = checkUserEnrollment(userId, courseId);

            if (!isEnrolled) {
                maskAllModuleLessons(modules);
            }

            return ResponseEntity.ok(modules);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(null); // Invalid token — fail-safe
        }
    }

    private boolean hasAdminOrCreatorAccess(String token, String userId, String courseId) {
        try {
            String role = jwtUtil.extractRole(token);

            if ("COURSE_ADMIN".equals(role) || "ADMIN".equals(role)) {
                return true;
            }

            CourseResponseDTO course = courseService.getCourseDetail(courseId);
            return course != null && userId.equals(course.getCreatorId());

        } catch (Exception e) {
            log.warn("Failed to check role or creator for courseId={}", courseId, e);
            return false;
        }
    }

    private boolean checkUserEnrollment(String userId, String courseId) {
        try {
            com.example.course_service.dto.response.AccessCheckResponse accessCheck = enrollmentClient.checkAccess(userId, courseId, "RECORDED_COURSE");
            return accessCheck != null && accessCheck.isHasAccess();
        } catch (Exception e) {
            log.warn("Enrollment check failed");
            return false; // Fail-safe: assume not enrolled
        }
    }

    private void maskAllModuleLessons(List<ModuleResponseDTO> modules) {
        for (ModuleResponseDTO module : modules) {
            if (module.getLessons() != null) {
                for (LessonResponseDTO lesson : module.getLessons()) {
                    lesson.maskContent();
                }
            }
        }
    }
}