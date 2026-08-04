package com.example.course_service.dto.request;

import com.example.course_service.model.FileType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UploadInitRequest {
    @NotNull(message = "File type is required")
    private FileType fileType;
    private String fileName;
    private String contentType;
    private Long fileSize;
}
