package com.example.course_service.dto.request;

import lombok.Data;

@Data
public class MultipartUploadInitRequest {

    private String fileName;
    private String contentType;
    private String lessonId;
    private Long fileSize;

}