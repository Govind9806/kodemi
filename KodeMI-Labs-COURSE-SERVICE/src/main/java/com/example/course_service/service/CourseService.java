package com.example.course_service.service;
 
import com.example.course_service.dto.request.AbortMultipartUploadRequestDTO;
import com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO;
import com.example.course_service.dto.request.MultipartUploadInitRequest;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.MultipartUploadInitResponse;
import com.example.course_service.model.CourseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;
 
public interface CourseService {
 
    Map<String, Object> createCourse(String token ,CourseEntity request,MultipartFile demoVideo,MultipartFile thumbnail);
    Map<String, Object> createLiveCourse(String token ,CourseEntity request,MultipartFile demoVideo,MultipartFile thumbnail);
    List<CourseResponseDTO> getAllCourses();
    String updateCourse(String token, String courseId, CourseEntity course, MultipartFile thumbnail);
    String verifyCourse(String token, String courseId);
    String rejectCourse(String token, String courseId, String remarks);
    String moderateCourse(String token, String courseId, com.example.course_service.dto.request.CourseModerationRequest request);
    SseEmitter streamAllCoursesAdmin();
    List<CourseResponseDTO> getAllReviewedCourses(String token);
    List<CourseResponseDTO> getCoursesByCreatorId(String creatorId);
    List<CourseResponseDTO> getCoursesByCreatorId(String creatorId, String courseType);
 
    CourseResponseDTO getCourseDetail(String courseId);
    CourseResponseDTO getCourseDetailWithModules(String courseId);
    List<CourseResponseDTO> getCoursesByCategory(String categoryId);
    void refreshRatingCache(String courseId);
    void refreshCourseStats(String courseId);
    List<CourseResponseDTO> getAllCoursesForAdmin();
    List<CourseResponseDTO> getEnrolledCoursesForStudent(String token);

    MultipartUploadInitResponse initiateDemoVideoMultipartUpload(String token, MultipartUploadInitRequest request);
    String generateDemoVideoPresignedUrl(String token, String uploadId, String fileKey, int partNumber);
    CompleteMultipartUploadResponse completeDemoVideoMultipartUpload(String token, CompleteMultipartUploadRequestDTO request);
    String abortDemoVideoMultipartUpload(String token, AbortMultipartUploadRequestDTO request);
}