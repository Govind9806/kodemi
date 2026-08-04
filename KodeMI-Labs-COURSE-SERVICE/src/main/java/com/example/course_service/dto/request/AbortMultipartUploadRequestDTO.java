package com.example.course_service.dto.request;

import lombok.Data;

@Data
public class AbortMultipartUploadRequestDTO {

    private String uploadId;
    private String fileKey;

}