package com.example.course_service.service;

import com.example.course_service.dto.request.UploadInitRequest;
import com.example.course_service.dto.request.UploadPresignedUrlRequest;
import com.example.course_service.dto.request.UploadCompleteRequest;
import com.example.course_service.dto.request.UploadAbortRequest;
import com.example.course_service.dto.response.UploadInitResponse;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.model.FileType;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    UploadInitResponse initiateMultipartUpload(String token, UploadInitRequest request);
    String generatePresignedUrl(String token, UploadPresignedUrlRequest request);
    CompleteMultipartUploadResponse completeMultipartUpload(String token, UploadCompleteRequest request);
    String abortMultipartUpload(String token, UploadAbortRequest request);
    
    void validateAndConsumeUpload(String fileKey, String ownerId, String linkedEntityId);
    
    String uploadFileDirectly(String token, MultipartFile file, FileType fileType, String linkedEntityId);
}
