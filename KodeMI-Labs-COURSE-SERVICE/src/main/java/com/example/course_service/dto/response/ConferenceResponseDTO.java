package com.example.course_service.dto.response;

import lombok.Data;

@Data
public class ConferenceResponseDTO {
    private String conferenceId;
    private String organizerId;
    private String organizerName;
    private String title;
    private String description;
    private String actionType;
    private String sessionType;
    private String scheduledAt;
    private String startedAt;
    private Integer maxParticipants;
    private String roomId;
    private String status;
    private Boolean isRecording;
    private String createdAt;
    private String courseId;
    private String sourceType;
}
