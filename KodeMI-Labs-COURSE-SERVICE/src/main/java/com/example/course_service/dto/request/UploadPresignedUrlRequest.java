package com.example.course_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UploadPresignedUrlRequest {
    @NotBlank(message = "Upload ID is required")
    private String uploadId;
    
    @NotNull(message = "Part number is required")
    private Integer partNumber;
}
