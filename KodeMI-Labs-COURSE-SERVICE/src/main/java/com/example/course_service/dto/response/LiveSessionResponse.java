package com.example.course_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveSessionResponse {
    @com.fasterxml.jackson.annotation.JsonAlias({"sessionId", "liveClassId"})
    private String sessionId;
    private String courseId;
    private Instant startTime;
    private Instant endTime;
    private String joinLink;
    private String status;
}