package com.example.course_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadInitResponse {
    private String uploadId;
    private String fileKey;
    private String message;
}
