package com.example.course_service.feign;

import com.example.course_service.dto.response.AccessCheckResponse;
import com.example.course_service.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class EnrollmentClientFallbackFactory implements FallbackFactory<EnrollmentClient> {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentClientFallbackFactory.class);

    @Override
    public EnrollmentClient create(Throwable cause) {
        return new EnrollmentClient() {
            @Override
            public AccessCheckResponse checkAccess(String userId, String targetId, String targetType) {
                log.error("EnrollmentClient.checkAccess failed for userId: {}, targetId: {}, targetType: {}. Cause: {}", 
                        userId, targetId, targetType, cause.getMessage(), cause);
                throw new DownstreamServiceException("Enrollment service is currently unavailable. Access check failed.", cause);
            }

            @Override
            public List<String> getEnrolledLearners(String courseId) {
                log.warn("EnrollmentClient.getEnrolledLearners failed for courseId: {}. Falling back to empty list. Cause: {}", 
                        courseId, cause.getMessage(), cause);
                return Collections.emptyList();
            }

            @Override
            public List<com.example.course_service.dto.response.UserEnrollmentResponse> getUserEnrollmentsInternal(String userId) {
                log.warn("EnrollmentClient.getUserEnrollmentsInternal failed for userId: {}. Falling back to empty list. Cause: {}", 
                        userId, cause.getMessage(), cause);
                return Collections.emptyList();
            }
        };
    }
}
