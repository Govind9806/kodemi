package com.example.course_service.dto.request;

import lombok.Data;

@Data
public class CreateConferenceRequest {
    private String title;
    private String description;
    private String scheduledAt;
    private Integer maxParticipants;
    
    // Optional fields for LIVE course creation flow
    private String courseId;
    private String moduleId;
    private String lessonId;
    private String sourceType;
}
