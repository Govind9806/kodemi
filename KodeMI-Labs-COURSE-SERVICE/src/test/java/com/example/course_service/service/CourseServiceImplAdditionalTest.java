package com.example.course_service.service;

import com.example.course_service.dto.request.MultipartUploadInitRequest;
import com.example.course_service.dto.response.MultipartUploadInitResponse;
import com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO;
import com.example.course_service.dto.request.AbortMultipartUploadRequestDTO;
import com.example.course_service.dto.response.UploadInitResponse;
import com.example.course_service.dto.request.UploadInitRequest;
import com.example.course_service.dto.request.UploadPresignedUrlRequest;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.dto.request.UploadCompleteRequest;
import com.example.course_service.dto.request.UploadAbortRequest;
import com.example.course_service.exception.NullException;
import com.example.course_service.exception.ThumbnailNotFoundException;
import com.example.course_service.exception.TokenNotFoundException;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.feign.TrainerClient;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.dto.response.TrainerResponseDTO;
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
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseServiceImplAdditionalTest {

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
                courseRepository, categoryRepository, moduleRepository,
                lessonRepository, reviewRepository, trainerClient, enrollmentClient, fileService, uploadService, notificationPublisher, jwtUtil
        );

        com.example.course_service.dto.response.TrainerResponseDTO trainer = new com.example.course_service.dto.response.TrainerResponseDTO();
        trainer.setUserId("user-1");
        trainer.setFullName("Trainer");
        when(trainerClient.getTrainer(anyString())).thenReturn(trainer);
    }

    // ================= createCourse validation guards =================

    @Test
    void createCourse_NullToken_ThrowsTokenNotFoundException() {
        CourseEntity request = new CourseEntity();
        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);
        when(thumbnail.getOriginalFilename()).thenReturn("img.jpg");

        assertThrows(TokenNotFoundException.class,
                () -> courseService.createCourse(null, request, null, thumbnail));
    }

    @Test
    void createCourse_NullThumbnail_ThrowsThumbnailNotFoundException() {
        CourseEntity request = new CourseEntity();
        assertThrows(ThumbnailNotFoundException.class,
                () -> courseService.createCourse(TOKEN, request, null, null));
    }

    @Test
    void createCourse_NoDemoVideoAndNoKey_ThrowsIllegalArgument() throws Exception {
        CourseEntity request = new CourseEntity();
        request.setTitle("Java");
        // no demoVideoKey, no demoVideo file

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

        assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(TOKEN, request, null, thumbnail));
    }

    @Test
    void createCourse_InvalidThumbnailExtension_ThrowsIllegalArgument() throws Exception {
        CourseEntity request = new CourseEntity();
        request.setTitle("Java");

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);
        when(thumbnail.getOriginalFilename()).thenReturn("img.gif"); // not allowed

        assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(TOKEN, request, null, thumbnail));
    }



    // ================= createCourse with demoVideo file (not pre-uploaded key) =================

    @Test
    void createCourse_WithDemoVideoFile_Success() throws Exception {
        CourseEntity request = new CourseEntity();
        request.setTitle("Java");
        // no demoVideoKey — will use the file

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);
        when(thumbnail.getOriginalFilename()).thenReturn("img.jpg");
        when(thumbnail.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(thumbnail.getSize()).thenReturn(4L);
        when(thumbnail.getContentType()).thenReturn("image/jpeg");

        MultipartFile demoVideo = mock(MultipartFile.class);
        when(demoVideo.isEmpty()).thenReturn(false);
        when(demoVideo.getOriginalFilename()).thenReturn("demo.mp4");
        when(demoVideo.getInputStream()).thenReturn(new ByteArrayInputStream("video".getBytes()));
        when(demoVideo.getSize()).thenReturn(5L);
        when(demoVideo.getContentType()).thenReturn("video/mp4");

        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("user-1");
        trainer.setFullName("Trainer");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        java.util.Map<String, Object> result = courseService.createCourse(TOKEN, request, demoVideo, thumbnail);

        assertTrue(result.get("message").toString().startsWith("Course Created Successfully"));
        verify(courseRepository).save(any(CourseEntity.class));
    }



    // ================= createLiveCourse validation guards =================

    @Test
    void createLiveCourse_NullRequest_ThrowsNullException() {
        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);

        assertThrows(NullException.class,
                () -> courseService.createLiveCourse(TOKEN, null, null, thumbnail));
    }

    @Test
    void createLiveCourse_NullThumbnail_ThrowsThumbnailNotFoundException() {
        CourseEntity request = new CourseEntity();
        assertThrows(ThumbnailNotFoundException.class,
                () -> courseService.createLiveCourse(TOKEN, request, null, null));
    }

    @Test
    void createLiveCourse_NullToken_ThrowsTokenNotFoundException() {
        CourseEntity request = new CourseEntity();
        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);
        when(thumbnail.getOriginalFilename()).thenReturn("img.jpg");

        assertThrows(TokenNotFoundException.class,
                () -> courseService.createLiveCourse(null, request, null, thumbnail));
    }

    // ================= updateCourse validation =================

    @Test
    void updateCourse_NullRequest_ThrowsNullException() {
        assertThrows(NullException.class,
                () -> courseService.updateCourse("user-1", "C101", null, null));
    }

    @Test
    void updateCourse_WithNewThumbnail_UploadsAndSaves() throws java.io.IOException {
        CourseEntity existing = new CourseEntity();
        existing.setCourseId("C101");
        existing.setCreatorId("user-1");
        when(courseRepository.findById("C101")).thenReturn(existing);

        CourseEntity request = new CourseEntity();
        request.setTitle("Updated Title");

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);
        when(thumbnail.getOriginalFilename()).thenReturn("new-thumb.png");
        when(thumbnail.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(thumbnail.getSize()).thenReturn(4L);
        when(thumbnail.getContentType()).thenReturn("image/png");

        String result = courseService.updateCourse("user-1", "C101", request, thumbnail);

        assertTrue(result.startsWith("Course Updated Successfully."));
        verify(courseRepository).save(existing);
    }

    // ================= refreshRatingCache with null ratings =================

    @Test
    void refreshRatingCache_ReviewWithNullRating_TreatedAsZero() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        com.example.course_service.model.ReviewEntity r = new com.example.course_service.model.ReviewEntity();
        r.setRating(null);

        when(courseRepository.findById("C101")).thenReturn(course);
        when(reviewRepository.findByCourseId("C101")).thenReturn(List.of(r));

        courseService.refreshRatingCache("C101");

        verify(courseRepository).save(course);
        assertEquals(0.0, course.getAverageRating());
    }

    // ================= refreshCourseStats edge cases =================

    @Test
    void refreshCourseStats_NoModules_SetsZeroLessons() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        when(courseRepository.findById("C101")).thenReturn(course);
        when(moduleRepository.findByCourseId("C101")).thenReturn(List.of());

        courseService.refreshCourseStats("C101");

        verify(courseRepository).save(course);
        assertEquals(0, course.getLessonCount());
        assertEquals("0 Minutes", course.getDurationLabel());
    }

    @Test
    void refreshCourseStats_ExactlyOneHour_FormatsCorrectly() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        com.example.course_service.model.ModuleEntity module = new com.example.course_service.model.ModuleEntity();
        module.setModuleId("M1");

        com.example.course_service.model.LessonEntity lesson = new com.example.course_service.model.LessonEntity();
        lesson.setDuration(60);

        when(courseRepository.findById("C101")).thenReturn(course);
        when(moduleRepository.findByCourseId("C101")).thenReturn(List.of(module));
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(lesson));

        courseService.refreshCourseStats("C101");

        assertEquals("1 Hour", course.getDurationLabel());
    }

    @Test
    void refreshCourseStats_MultipleHours_FormatsCorrectly() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        com.example.course_service.model.ModuleEntity module = new com.example.course_service.model.ModuleEntity();
        module.setModuleId("M1");

        com.example.course_service.model.LessonEntity lesson = new com.example.course_service.model.LessonEntity();
        lesson.setDuration(120);

        when(courseRepository.findById("C101")).thenReturn(course);
        when(moduleRepository.findByCourseId("C101")).thenReturn(List.of(module));
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(lesson));

        courseService.refreshCourseStats("C101");

        assertEquals("2 Hours", course.getDurationLabel());
    }


    // ================= getCourseDetail with null category =================

    @Test
    void getCourseDetail_NullCategoryId_HandlesGracefully() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");
        course.setCategoryId(null);

        when(courseRepository.findById("C101")).thenReturn(course);
        when(reviewRepository.findByCourseId("C101")).thenReturn(List.of());
        when(fileService.generateDownloadUrl(any())).thenReturn("url");

        var result = courseService.getCourseDetail("C101");

        assertNotNull(result);
        assertNull(result.getCategoryName());
    }

    // ================= Demo Video Multipart Upload Tests =================

    @Test
    void initiateDemoVideoMultipartUpload_Success() {
        MultipartUploadInitRequest request = new MultipartUploadInitRequest();
        request.setFileName("demo.mp4");
        request.setContentType("video/mp4");
        request.setFileSize(100L);

        UploadInitResponse mockRes = new UploadInitResponse("up123", "key123", "success");
        when(uploadService.initiateMultipartUpload(eq(TOKEN), any(UploadInitRequest.class))).thenReturn(mockRes);

        MultipartUploadInitResponse result = courseService.initiateDemoVideoMultipartUpload(TOKEN, request);

        assertNotNull(result);
        assertEquals("up123", result.getUploadId());
        assertEquals("key123", result.getFileKey());
    }

    @Test
    void generateDemoVideoPresignedUrl_Success() {
        when(uploadService.generatePresignedUrl(eq(TOKEN), any(UploadPresignedUrlRequest.class))).thenReturn("url123");

        String result = courseService.generateDemoVideoPresignedUrl(TOKEN, "up123", "key123", 1);

        assertEquals("url123", result);
    }

    @Test
    void completeDemoVideoMultipartUpload_Success() {
        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setUploadId("up123");
        CompleteMultipartUploadRequestDTO.PartETag p = new CompleteMultipartUploadRequestDTO.PartETag();
        p.setPartNumber(1);
        p.setETag("etag123");
        request.setParts(List.of(p));

        CompleteMultipartUploadResponse mockRes = new CompleteMultipartUploadResponse("up123", "key123", "completed");
        when(uploadService.completeMultipartUpload(eq(TOKEN), any(UploadCompleteRequest.class))).thenReturn(mockRes);

        CompleteMultipartUploadResponse result = courseService.completeDemoVideoMultipartUpload(TOKEN, request);

        assertNotNull(result);
        assertEquals("key123", result.getVideoKey());
    }

    @Test
    void abortDemoVideoMultipartUpload_Success() {
        AbortMultipartUploadRequestDTO request = new AbortMultipartUploadRequestDTO();
        request.setUploadId("up123");

        String result = courseService.abortDemoVideoMultipartUpload(TOKEN, request);

        assertEquals("Upload aborted", result);
        verify(uploadService, times(1)).abortMultipartUpload(eq(TOKEN), any(UploadAbortRequest.class));
    }
}
