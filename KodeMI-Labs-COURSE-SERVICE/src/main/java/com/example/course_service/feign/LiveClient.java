package com.example.course_service.feign;
import com.example.course_service.dto.request.LiveSessionRequest;
import com.example.course_service.dto.response.LiveSessionResponse;
import com.example.course_service.dto.request.CreateConferenceRequest;
import com.example.course_service.dto.response.ConferenceResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.List;

@FeignClient(name = "live-classes-service", fallbackFactory = LiveClientFallbackFactory.class)
public interface LiveClient {

    @PostMapping("/live-classes/create")
    LiveSessionResponse createSession(@RequestBody LiveSessionRequest request);

    @PostMapping("/api/v1/conferences/create")
    ConferenceResponseDTO createConference(@RequestBody CreateConferenceRequest request, @RequestHeader("Authorization") String token);

    @GetMapping("/internal/session/{sessionId}")
    LiveSessionResponse getSessionById(@PathVariable("sessionId") String sessionId);

    @GetMapping("/internal/session/by-course/{courseId}")
    List<LiveSessionResponse> getSessionsByCourse(@PathVariable("courseId") String courseId);
}
