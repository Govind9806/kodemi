package com.example.course_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachRecordingRequest {
    private String courseId;
    private String liveSessionId;
    private String recordingUrl;
}

