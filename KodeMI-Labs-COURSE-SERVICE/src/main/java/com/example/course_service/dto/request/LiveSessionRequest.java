package com.example.course_service.dto.request;

import lombok.Data;
import java.util.Date;

@Data
public class LiveSessionRequest {

    private String title;
    private String description;
    private String courseId;
    private Date scheduledAt;
    private Integer duration;
    private Integer maxParticipants;
    private String organizerId;
    private String organizerName;

}