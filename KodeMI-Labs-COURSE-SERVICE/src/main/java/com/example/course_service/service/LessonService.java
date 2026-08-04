package com.example.course_service.service;
 
import com.example.course_service.dto.request.*;
import com.example.course_service.dto.response.LessonResponseDTO;
import com.example.course_service.dto.response.MultipartUploadInitResponse;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
 
import java.util.List;
import java.util.Map;
 
public interface LessonService {
 
    Map<String, Object> createLesson(Object request, String token);
 
    String updateLesson(String token, String lessonId, Object request);
 
    Map<String, String> generateUploadUrl(String token, String lessonId, String fileName);
 
    MultipartUploadInitResponse initiateMultipartUpload(String token, MultipartUploadInitRequest request);
 
    String generatePartUploadUrl(String token, String uploadId, String fileKey, int partNumber);
 
    CompleteMultipartUploadResponse completeMultipartUpload(String token, CompleteMultipartUploadRequestDTO request);
 
    String abortMultipartUpload(String token, AbortMultipartUploadRequestDTO request);
 
    String saveUploadedContent(String token, SaveUploadedContentRequest request);
 
    LessonResponseDTO getLessonById(String lessonId, String token);
 
    String getDownloadUrl(String token, String lessonId, String fileKey);
 
    List<LessonResponseDTO> getLessonsByModule(String moduleId, String token);
 
    // 🔥 LIVE SUPPORT
    String saveLiveRecording(LiveRecordingRequest request);
}