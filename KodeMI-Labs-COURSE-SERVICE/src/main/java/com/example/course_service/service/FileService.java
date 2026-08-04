package com.example.course_service.service;
 
import com.example.course_service.dto.request.MultipartUploadPartETag;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import java.io.InputStream;
import java.util.List;
 
public interface FileService {
 
    String generateUploadUrl(String key, String contentType);
 
    String generatePresignedUrl(String key);
 
    String generateDownloadUrl(String key);
 
    void deleteFile(String key);
 
    void uploadFile(String key, InputStream inputStream, long fileSize, String contentType);
 
    boolean fileExists(String key);
 
    String getBucketName();
 
    // Multipart support (keep for other potential uses)
    String initiateMultipartUpload(String key);
    String generatePartUploadUrl(String fileKey, String uploadId, int partNumber);
    CompleteMultipartUploadResponse completeMultipartUpload(String key, String uploadId, List<MultipartUploadPartETag> parts);
    void abortMultipartUpload(String key, String uploadId);
}
