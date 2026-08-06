package com.example.course_service.service;

import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.TrainerResponseDTO;
import com.example.course_service.exception.CourseNotFoundException;
import com.example.course_service.exception.NullException;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.feign.TrainerClient;
import com.example.course_service.model.CategoryEntity;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.ModuleRepository;
import com.example.course_service.repository.ReviewRepository;
import com.example.course_service.service.impl.CourseServiceImpl;
import com.example.course_service.service.notification.NotificationPublisher;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    @SuppressWarnings("unchecked")
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
        ObjectProvider<CourseServiceImpl> selfProvider = mock(ObjectProvider.class);

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
                jwtUtil,
                selfProvider
        );
        when(selfProvider.getIfAvailable(any())).thenReturn(courseService);
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
    void createLiveCourse_Success() throws IOException {
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

        Map<String, Object> result = courseService.createLiveCourse(TOKEN, request, null, thumbnail);

        assertTrue(result.get("message").toString().startsWith("Live Course Created Successfully"));
        verify(courseRepository).save(any(CourseEntity.class));
    }

    @Test
    void createLiveCourse_NullRequest_ThrowsException() {
        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);

        assertThrows(NullException.class,
                () -> courseService.createLiveCourse(TOKEN, null, null, thumbnail));
    }
}