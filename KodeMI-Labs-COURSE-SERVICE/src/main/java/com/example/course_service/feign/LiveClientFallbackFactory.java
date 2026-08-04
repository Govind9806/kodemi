package com.example.course_service.feign;

import com.example.course_service.dto.request.LiveSessionRequest;
import com.example.course_service.dto.response.LiveSessionResponse;
import com.example.course_service.dto.request.CreateConferenceRequest;
import com.example.course_service.dto.response.ConferenceResponseDTO;
import com.example.course_service.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class LiveClientFallbackFactory implements FallbackFactory<LiveClient> {

    private static final Logger log = LoggerFactory.getLogger(LiveClientFallbackFactory.class);

    @Override
    public LiveClient create(Throwable cause) {
        return new LiveClient() {
            @Override
            public LiveSessionResponse createSession(LiveSessionRequest request) {
                log.error("LiveClient.createSession failed. Fail-closed. Cause: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Live classes service is currently unavailable. Failed to create session.", cause);
            }

            @Override
            public ConferenceResponseDTO createConference(CreateConferenceRequest request, String token) {
                log.error("LiveClient.createConference failed. Fail-closed. Cause: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Live classes service is currently unavailable. Failed to create conference.", cause);
            }

            @Override
            public LiveSessionResponse getSessionById(String sessionId) {
                log.warn("LiveClient.getSessionById failed for sessionId: {}. Falling back to empty session response. Cause: {}", 
                        sessionId, cause.getMessage(), cause);
                return new LiveSessionResponse();
            }

            @Override
            public List<LiveSessionResponse> getSessionsByCourse(String courseId) {
                log.warn("LiveClient.getSessionsByCourse failed for courseId: {}. Falling back to empty list. Cause: {}", 
                        courseId, cause.getMessage(), cause);
                return Collections.emptyList();
            }
        };
    }
}
