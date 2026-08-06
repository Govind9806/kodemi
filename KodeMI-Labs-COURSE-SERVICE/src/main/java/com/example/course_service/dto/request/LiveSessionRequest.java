package com.example.course_service.dto.request;

import lombok.Data;

import java.time.Instant;

@Data
public class LiveSessionRequest {

    private String title;
    private String description;
    private String courseId;
    private Instant scheduledAt;
    private Integer duration;
    private Integer maxParticipants;
    private String organizerId;
    private String organizerName;

}