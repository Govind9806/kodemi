package com.example.course_service.dto.response;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteMultipartUploadResponse {
 
    private String uploadId;
    private String videoKey;
    private String message;
 
}
