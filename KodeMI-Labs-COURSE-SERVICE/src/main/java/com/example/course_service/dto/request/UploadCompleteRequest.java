package com.example.course_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class UploadCompleteRequest {
    @NotBlank(message = "Upload ID is required")
    private String uploadId;
    
    @NotEmpty(message = "Parts are required")
    private List<MultipartUploadPartETag> parts;
}
