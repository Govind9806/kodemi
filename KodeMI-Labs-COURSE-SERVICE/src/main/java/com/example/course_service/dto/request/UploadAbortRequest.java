package com.example.course_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadAbortRequest {
    @NotBlank(message = "Upload ID is required")
    private String uploadId;
}
