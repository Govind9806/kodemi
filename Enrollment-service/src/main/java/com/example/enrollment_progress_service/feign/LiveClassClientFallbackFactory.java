package com.example.enrollment_progress_service.feign;

import com.example.enrollment_progress_service.dto.response.EnrollmentItemInfoResponse;
import com.example.enrollment_progress_service.exception.DownstreamServiceException;
import com.example.enrollment_progress_service.feign.LiveClassClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LiveClassClientFallbackFactory implements FallbackFactory<LiveClassClient> {
    @Override
    public LiveClassClient create(Throwable cause) {
        return new LiveClassClient() {
            @Override
            public EnrollmentItemInfoResponse getConferenceEnrollmentInfo(String conferenceId) {
                log.error("Live class service call failed for getConferenceEnrollmentInfo with conferenceId: {}. Reason: {}", conferenceId, cause.getMessage(), cause);
                throw new DownstreamServiceException("Live class service is currently unavailable. Please try again later.", cause);
            }
        };
    }
}
