package com.example.course_service.dto.request;

import lombok.Data;

@Data
public class SaveUploadedContentRequest {

    private String lessonId;
    private String fileKey;
    private String label; // e.g. "Intro Video"

}