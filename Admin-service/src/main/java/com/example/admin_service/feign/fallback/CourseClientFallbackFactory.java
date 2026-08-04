package com.example.admin_service.feign.fallback;

import com.example.admin_service.feign.CourseClient;
import com.example.admin_service.dto.response.CourseResponseDTO;
import com.example.admin_service.dto.request.CourseModerationRequest;
import com.example.admin_service.exceptions.DownstreamServiceException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CourseClientFallbackFactory implements FallbackFactory<CourseClient> {
    @Override
    public CourseClient create(Throwable cause) {
        return new CourseClient() {
            @Override
            public Map<String, Object> reviewCourse(String token, String courseId, CourseModerationRequest request) {
                log.error("CourseClient reviewCourse failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Course service is currently unavailable.", cause);
            }

            @Override
            public List<CourseResponseDTO> getAllCoursesAdmin(String token) {
                log.error("CourseClient getAllCoursesAdmin failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Course service is currently unavailable.", cause);
            }
        };
    }
}
