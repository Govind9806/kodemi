package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.response.CourseResponseDTO;
import com.example.enrollment_progress_service.dto.response.EnrollmentItemInfoResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CourseClientFallbackFactory implements FallbackFactory<CourseClient> {

    @Override
    public CourseClient create(Throwable cause) {
        return new CourseClient() {

            @Override
            public EnrollmentItemInfoResponse getCourseEnrollmentInfo(String courseId) {
                log.error("Course service fallback triggered for enrollment info. courseId={}", courseId, cause);
                return null;
            }

            @Override
            public CourseResponseDTO getCourseDetail(String courseId) {
                log.error("Course service fallback triggered for course detail. courseId={}", courseId, cause);
                return null;
            }

            @Override
            public java.util.List<com.example.enrollment_progress_service.dto.response.ReviewResponseDTO> getReviewsByCourse(String courseId) {
                log.error("Course service fallback triggered for reviews. courseId={}", courseId, cause);
                return java.util.Collections.emptyList();
            }
        };
    }
}