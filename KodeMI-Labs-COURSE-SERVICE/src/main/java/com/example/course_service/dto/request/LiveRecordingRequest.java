package com.example.course_service.dto.request;
import lombok.Data;

@Data
public class LiveRecordingRequest {
    private String lessonId;
    private String recordingKey;
    private Integer duration;
}