package com.example.course_service.controller;

import com.example.course_service.component.RequiresRole;
import com.example.course_service.dto.request.CourseModerationRequest;
import com.example.course_service.dto.response.AccessCheckResponse;
import com.example.course_service.dto.response.CourseModerationResponse;
import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.EnrollmentItemInfoResponse;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.service.CourseService;
import com.example.course_service.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/course")
public class CourseController {

    private static final String MESSAGE = "message";

    // Roles
    private static final String ROLE_TRAINER = "TRAINER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_COURSE_ADMIN = "COURSE_ADMIN";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    // Course type mapping
    private static final String COURSE_TYPE_RECORDED = "RECORDED";
    private static final String COURSE_TYPE_LIVE = "LIVE";
    private static final String TARGET_TYPE_RECORDED_COURSE = "RECORDED_COURSE";
    private static final String TARGET_TYPE_LIVE_COURSE = "LIVE_COURSE";

    // Pricing
    private static final String PRICING_TYPE_PAID = "PAID";
    private static final String PRICING_TYPE_FREE = "FREE";

    private final CourseService courseService;
    private final JwtUtil jwtUtil;
    private final EnrollmentClient enrollmentClient;

    // This endpoint is used to create recorded courses.
    // Pass demo video file directly or pass pre-uploaded fileKey as "demoVideoKey" inside the course JSON.
    @RequiresRole(ROLE_TRAINER)
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createCourse(
            @RequestHeader("Authorization") String token,
            @RequestPart("course") CourseEntity request,
            @RequestPart(value = "demoVideo", required = false) MultipartFile demoVideo,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        return ResponseEntity.ok(courseService.createCourse(token, request, demoVideo, thumbnail));
    }

    // This endpoint is used to create a LIVE course.
    // Pass demo video file directly or pass pre-uploaded fileKey as "demoVideoKey" inside the course JSON.
    @RequiresRole(ROLE_TRAINER)
    @PostMapping(value = "/create-live", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createLiveCourse(
            @RequestHeader("Authorization") String token,
            @RequestPart("course") CourseEntity request,
            @RequestPart(value = "demoVideo", required = false) MultipartFile demoVideo,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        return ResponseEntity.ok(courseService.createLiveCourse(token, request, demoVideo, thumbnail));
    }

    // This endpoint is used to update both recorded and LIVE courses. For LIVE courses, it updates the
    // LIVE-specific details; for recorded courses, it updates the standard course details. The frontend
    // will call this endpoint to update the course when user clicks on the edit button on the course detail page.
    @RequiresRole(ROLE_TRAINER)
    @PatchMapping(value = "/edit/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> updateCourse(
            @RequestHeader("Authorization") String token,
            @PathVariable String courseId,
            @RequestPart("course") CourseEntity request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        return ResponseEntity.ok(Map.of(MESSAGE,
                courseService.updateCourse(token, courseId, request, thumbnail)));
    }

    @RequiresRole(ROLE_COURSE_ADMIN)
    @GetMapping("/get-all-courses")
    public ResponseEntity<List<CourseResponseDTO>> getAllCoursesAdmin() {
        return ResponseEntity.ok(courseService.getAllCoursesForAdmin());
    }

    @RequiresRole(ROLE_COURSE_ADMIN)
    @PutMapping("/review/{courseId}")
    public ResponseEntity<CourseModerationResponse> reviewCourse(
            @RequestHeader("Authorization") String token,
            @PathVariable String courseId,
            @RequestBody CourseModerationRequest request) {

        log.info("Review API hit for courseId={}, action={}", courseId, request.getAction());
        String result = courseService.moderateCourse(token, courseId, request);
        boolean verified = "VERIFY".equalsIgnoreCase(request.getAction());
        log.info("Course {} review complete: action={}, isVerified={}", courseId, request.getAction(), verified);
        return ResponseEntity.ok(
                CourseModerationResponse.builder()
                        .courseId(courseId)
                        .action(request.getAction())
                        .isVerified(verified)
                        .message(result)
                        .build()
        );
    }

    // this endpoint is used to get all courses, it will return the list of all courses
    // Only Verified Courses (public endpoint)
    @GetMapping("/get-all")
    public ResponseEntity<List<CourseResponseDTO>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/enrolled")
    public ResponseEntity<List<CourseResponseDTO>> getEnrolledCourses(
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(courseService.getEnrolledCoursesForStudent(token));
    }

    // this endpoint is used to get course details, it will return the course details.
    @GetMapping("/get-detail/{courseId}")
    public ResponseEntity<CourseResponseDTO> getCourseDetail(
            @PathVariable String courseId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        CourseResponseDTO detail = courseService.getCourseDetailWithModules(courseId);

        if (token == null || token.isBlank()) {
            detail.maskVideoContent();
            return ResponseEntity.ok(detail);
        }

        try {
            String userId = jwtUtil.extractUserId(token);
            String role = extractRoleSafely(token);

            if (isOrganizerOrAdmin(role, userId, detail.getCreatorId())) {
                return ResponseEntity.ok(detail);
            }

            String courseType = resolveTargetType(detail.getCourseType());
            boolean isEnrolled = checkEnrollmentAccess(userId, courseId, courseType);

            if (!isEnrolled) {
                detail.maskVideoContent();
            }
        } catch (Exception e) {
            log.warn("Token parsing or authentication failed, falling back to public view", e);
            detail.maskVideoContent();
        }

        return ResponseEntity.ok(detail);
    }

    // this endpoint is used to get courses by category, it will return the list of courses by category.
    @GetMapping("/get-by-category/{categoryId}")
    public ResponseEntity<List<CourseResponseDTO>> getCoursesByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(courseService.getCoursesByCategory(categoryId));
    }

    @GetMapping("/get-detail/protected/{courseId}")
    public ResponseEntity<CourseResponseDTO> getProtectedCourseDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable String courseId) {

        String userId = jwtUtil.extractUserId(token);
        CourseResponseDTO detail = courseService.getCourseDetailWithModules(courseId);

        String role = extractRoleSafely(token);
        if (isOrganizerOrAdmin(role, userId, detail.getCreatorId())) {
            return ResponseEntity.ok(detail);
        }

        String courseType = resolveTargetType(detail.getCourseType());
        boolean isEnrolled = checkEnrollmentAccess(userId, courseId, courseType);

        if (!isEnrolled) {
            detail.maskVideoContent();
        }
        return ResponseEntity.ok(detail);
    }

    @RequiresRole(ROLE_TRAINER)
    @GetMapping("/instructor/courses")
    public ResponseEntity<List<CourseResponseDTO>> getInstructorCourses(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "courseType", required = false) String courseType) {
        String userId = jwtUtil.extractUserId(token);
        List<CourseResponseDTO> courses = courseService.getCoursesByCreatorId(userId, courseType);
        return ResponseEntity.ok(courses);
    }

    // This endpoint is used to refresh the rating after reviews are edited
    @RequiresRole(ROLE_ADMIN)
    @PostMapping("/internal/refresh-rating/{courseId}")
    public ResponseEntity<Map<String, String>> refreshRating(
            @PathVariable String courseId) {
        courseService.refreshRatingCache(courseId);
        return ResponseEntity.ok(Map.of(MESSAGE, "Rating refreshed."));
    }

    @RequiresRole(ROLE_ADMIN)
    @PostMapping("/internal/refresh-stats/{courseId}")
    public ResponseEntity<Map<String, String>> refreshStats(
            @PathVariable String courseId) {
        courseService.refreshCourseStats(courseId);
        return ResponseEntity.ok(Map.of(MESSAGE, "Stats refreshed."));
    }

    @GetMapping("/internal/enrollment-info/{courseId}")
    public ResponseEntity<EnrollmentItemInfoResponse> getEnrollmentInfo(
            @PathVariable String courseId) {
        CourseResponseDTO detail = courseService.getCourseDetail(courseId);
        String mappedType = resolveTargetType(detail.getCourseType());

        EnrollmentItemInfoResponse response = EnrollmentItemInfoResponse.builder()
                .targetId(detail.getCourseId())
                .targetType(mappedType)
                .title(detail.getTitle())
                .creatorId(detail.getCreatorId())
                .pricingType(detail.getPrice() != null && detail.getPrice() > 0 ? PRICING_TYPE_PAID : PRICING_TYPE_FREE)
                .price(detail.getPrice() != null ? BigDecimal.valueOf(detail.getPrice()) : BigDecimal.ZERO)
                .isFree(detail.getPrice() == null || detail.getPrice() == 0.0)
                .isVerified(detail.getIsVerified())
                .status(detail.getStatus())
                .build();
        return ResponseEntity.ok(response);
    }

    private String extractRoleSafely(String token) {
        try {
            return jwtUtil.extractRole(token);
        } catch (Exception e) {
            log.warn("Failed to extract role for token", e);
            return null;
        }
    }


    private boolean isOrganizerOrAdmin(String role, String userId, String creatorId) {
        return ROLE_COURSE_ADMIN.equals(role)
                || ROLE_SUPER_ADMIN.equals(role)
                || (userId != null && userId.equals(creatorId));
    }


    private boolean checkEnrollmentAccess(String userId, String courseId, String courseType) {
        try {
            AccessCheckResponse accessCheck = enrollmentClient.checkAccess(userId, courseId, courseType);
            return accessCheck != null && accessCheck.isHasAccess();
        } catch (Exception e) {
            log.warn("Enrollment check failed for userId={}, courseId={}", userId, courseId);
            return false;
        }
    }


    private String resolveTargetType(String courseType) {
        if (courseType == null) {
            return TARGET_TYPE_RECORDED_COURSE;
        }
        if (COURSE_TYPE_RECORDED.equalsIgnoreCase(courseType)) {
            return TARGET_TYPE_RECORDED_COURSE;
        }
        if (COURSE_TYPE_LIVE.equalsIgnoreCase(courseType)) {
            return TARGET_TYPE_LIVE_COURSE;
        }
        return courseType;
    }
}