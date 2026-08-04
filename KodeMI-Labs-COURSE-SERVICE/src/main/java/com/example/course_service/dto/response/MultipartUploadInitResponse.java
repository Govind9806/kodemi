package com.example.course_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MultipartUploadInitResponse {

    private String uploadId;
    private String fileKey;
    private String message;

}