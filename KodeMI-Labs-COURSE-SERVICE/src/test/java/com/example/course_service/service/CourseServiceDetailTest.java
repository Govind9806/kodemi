package com.example.course_service.service;

import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.TrainerResponseDTO;
import com.example.course_service.exception.CourseNotFoundException;
import com.example.course_service.feign.TrainerClient;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.model.CategoryEntity;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.repository.*;
import com.example.course_service.service.impl.CourseServiceImpl;
import com.example.course_service.service.UploadService;
import com.example.course_service.service.notification.NotificationPublisher;
import com.example.course_service.model.FileType;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CourseServiceDetailTest {

    private CourseRepository courseRepository;
    private CategoryRepository categoryRepository;
    private ModuleRepository moduleRepository;
    private LessonRepository lessonRepository;
    private ReviewRepository reviewRepository;
    private TrainerClient trainerClient;
    private EnrollmentClient enrollmentClient;
    private FileService fileService;
    private UploadService uploadService;
    private NotificationPublisher notificationPublisher;
    private JwtUtil jwtUtil;
    private CourseServiceImpl courseService;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setup() {
        courseRepository = mock(CourseRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        moduleRepository = mock(ModuleRepository.class);
        lessonRepository = mock(LessonRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        trainerClient = mock(TrainerClient.class);
        enrollmentClient = mock(EnrollmentClient.class);
        fileService = mock(FileService.class);
        uploadService = mock(UploadService.class);
        notificationPublisher = mock(NotificationPublisher.class);
        jwtUtil = mock(JwtUtil.class);

        courseService = new CourseServiceImpl(
                courseRepository,
                categoryRepository,
                moduleRepository,
                lessonRepository,
                reviewRepository,
                trainerClient,
                enrollmentClient,
                fileService,
                uploadService,
                notificationPublisher,
                jwtUtil
        );
    }

    // ================= GET COURSE DETAIL =================

    @Test
    void getCourseDetail_WithCategory_ReturnsDTO() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");
        course.setCategoryId("cat-1");
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        CategoryEntity category = new CategoryEntity();
        category.setName("Programming");

        when(courseRepository.findById("C101")).thenReturn(course);
        when(categoryRepository.findById("cat-1")).thenReturn(category);
        when(reviewRepository.findByCourseId("C101")).thenReturn(List.of());
        when(fileService.generateDownloadUrl(any())).thenReturn("url");

        CourseResponseDTO result = courseService.getCourseDetail("C101");

        assertNotNull(result);
        assertEquals("C101", result.getCourseId());
        assertEquals("Programming", result.getCategoryName());
    }

    @Test
    void getCourseDetail_NotFound_ThrowsException() {
        when(courseRepository.findById("C999")).thenReturn(null);

        assertThrows(CourseNotFoundException.class,
                () -> courseService.getCourseDetail("C999"));
    }

    // ================= CREATE LIVE COURSE =================

    @Test
    void createLiveCourse_Success() throws Exception {
        CourseEntity request = new CourseEntity();
        request.setTitle("Live Java");
        request.setDemoVideoKey("preview/demo-videos/existing-key.mp4");

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);
        when(thumbnail.getOriginalFilename()).thenReturn("img.jpg");
        when(thumbnail.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(thumbnail.getSize()).thenReturn(4L);
        when(thumbnail.getContentType()).thenReturn("image/jpeg");

        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("user-1");
        trainer.setFullName("Trainer");

        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        java.util.Map<String, Object> result = courseService.createLiveCourse(TOKEN, request, null, thumbnail);

        assertTrue(result.get("message").toString().startsWith("Live Course Created Successfully"));
        verify(courseRepository).save(any(CourseEntity.class));
    }

    @Test
    void createLiveCourse_NullRequest_ThrowsException() {
        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);

        assertThrows(Exception.class,
                () -> courseService.createLiveCourse(TOKEN, null, null, thumbnail));
    }

    // ================= MULTIPART DEMO VIDEO =================

    @Test
    void initiateDemoVideoMultipartUpload_ReturnsResponse() {
        com.example.course_service.dto.response.TrainerResponseDTO trainer = new com.example.course_service.dto.response.TrainerResponseDTO();
        trainer.setUserId("user-1");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        com.example.course_service.dto.response.UploadInitResponse mockRes = new com.example.course_service.dto.response.UploadInitResponse("upload-123", "key", "INITIATED");
        when(uploadService.initiateMultipartUpload(eq(TOKEN), any(com.example.course_service.dto.request.UploadInitRequest.class))).thenReturn(mockRes);

        var result = courseService.initiateDemoVideoMultipartUpload(TOKEN,
                new com.example.course_service.dto.request.MultipartUploadInitRequest());

        assertNotNull(result);
        assertEquals("upload-123", result.getUploadId());
    }

    @Test
    void generateDemoVideoPresignedUrl_ReturnsUrl() {
        when(uploadService.generatePresignedUrl(eq(TOKEN), any())).thenReturn("https://presigned.url");

        String url = courseService.generateDemoVideoPresignedUrl(TOKEN, "upload-123", "key", 1);

        assertEquals("https://presigned.url", url);
    }

    @Test
    void abortDemoVideoMultipartUpload_CallsService() {
        com.example.course_service.dto.request.AbortMultipartUploadRequestDTO request =
                new com.example.course_service.dto.request.AbortMultipartUploadRequestDTO();
        request.setFileKey("key");
        request.setUploadId("upload-123");

        String result = courseService.abortDemoVideoMultipartUpload(TOKEN, request);

        assertEquals("Upload aborted", result);
        verify(uploadService).abortMultipartUpload(eq(TOKEN), any(com.example.course_service.dto.request.UploadAbortRequest.class));
    }

    @Test
    void completeDemoVideoMultipartUpload_ReturnsResponse() {
        com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO request =
                new com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO();
        request.setFileKey("key");
        request.setUploadId("upload-123");

        com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO.PartETag part =
                new com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO.PartETag();
        part.setPartNumber(1);
        part.setETag("etag-1");
        request.setParts(List.of(part));

        com.example.course_service.dto.response.CompleteMultipartUploadResponse mockResponse =
                new com.example.course_service.dto.response.CompleteMultipartUploadResponse("upload-123", "key", "done");
        when(uploadService.completeMultipartUpload(eq(TOKEN), any())).thenReturn(mockResponse);

        var result = courseService.completeDemoVideoMultipartUpload(TOKEN, request);

        assertNotNull(result);
        assertEquals("key", result.getVideoKey());
    }
}
