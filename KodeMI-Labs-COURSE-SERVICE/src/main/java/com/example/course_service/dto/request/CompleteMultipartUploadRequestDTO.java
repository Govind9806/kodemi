package com.example.course_service.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CompleteMultipartUploadRequestDTO {

    private String uploadId;
    private String fileKey;
    private List<PartETag> parts;

    @Data
    public static class PartETag {
        private Integer partNumber;
        private String eTag;
    }
}