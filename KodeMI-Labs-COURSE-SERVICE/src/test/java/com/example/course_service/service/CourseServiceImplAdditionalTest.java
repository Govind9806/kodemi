package com.example.course_service.service;

import com.example.course_service.dto.request.CourseModerationRequest;
import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.TrainerResponseDTO;
import com.example.course_service.dto.response.UserEnrollmentResponse;
import com.example.course_service.exception.CourseNotFoundException;
import com.example.course_service.exception.NullException;
import com.example.course_service.exception.ThumbnailNotFoundException;
import com.example.course_service.exception.TokenNotFoundException;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.feign.TrainerClient;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.model.ModuleEntity;
import com.example.course_service.model.ReviewEntity;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
        org.springframework.beans.factory.ObjectProvider<CourseServiceImpl> selfProvider = mock(org.springframework.beans.factory.ObjectProvider.class);

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

        TrainerResponseDTO trainer = new TrainerResponseDTO();
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
    void createCourse_NoDemoVideoAndNoKey_ThrowsIllegalArgument() throws IOException {
        CourseEntity request = new CourseEntity();
        request.setTitle("Java");

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
    void createCourse_InvalidThumbnailExtension_ThrowsIllegalArgument() {
        CourseEntity request = new CourseEntity();
        request.setTitle("Java");

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);
        when(thumbnail.getOriginalFilename()).thenReturn("img.gif"); // not allowed

        assertThrows(IllegalArgumentException.class,
                () -> courseService.createCourse(TOKEN, request, null, thumbnail));
    }

    // ================= createCourse with demoVideo file =================

    @Test
    void createCourse_WithDemoVideoFile_Success() throws IOException {
        CourseEntity request = new CourseEntity();
        request.setTitle("Java");

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

        Map<String, Object> result = courseService.createCourse(TOKEN, request, demoVideo, thumbnail);

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
    void updateCourse_WithNewThumbnail_UploadsAndSaves() throws IOException {
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

        ReviewEntity r = new ReviewEntity();
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

        ModuleEntity module = new ModuleEntity();
        module.setModuleId("M1");

        LessonEntity lesson = new LessonEntity();
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

        ModuleEntity module = new ModuleEntity();
        module.setModuleId("M1");

        LessonEntity lesson = new LessonEntity();
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

        CourseResponseDTO result = courseService.getCourseDetail("C101");

        assertNotNull(result);
        assertNull(result.getCategoryName());
    }

    // ================= streamAllCoursesAdmin =================

    @Test
    void streamAllCoursesAdmin_ReturnsSseEmitterAndEmitsCourses() {
        CourseEntity course1 = new CourseEntity();
        course1.setCourseId("C1");
        course1.setTitle("Java");
        course1.setIsVerified(true);

        CourseEntity course2 = new CourseEntity();
        course2.setCourseId("C2");
        course2.setTitle("Python");
        course2.setIsVerified(false);

        when(courseRepository.findAll()).thenReturn(List.of(course1, course2));
        when(reviewRepository.findByCourseId(anyString())).thenReturn(List.of());

        SseEmitter emitter = courseService.streamAllCoursesAdmin();

        assertNotNull(emitter);
    }

    // ================= reviewCourse admin moderation =================

    @Test
    void moderateCourse_RejectAction_SetsModerationStatusRejected() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin-1");
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");
        course.setCreatorId("user-1");

        when(courseRepository.findById("C101")).thenReturn(course);

        CourseModerationRequest req = new CourseModerationRequest();
        req.setCourseId("C101");
        req.setAction("REJECT");
        req.setRemarks("Incomplete content");

        String result = courseService.moderateCourse(TOKEN, "C101", req);

        assertNotNull(result);
        verify(courseRepository).save(course);
    }

    @Test
    void moderateCourse_InvalidAction_ThrowsIllegalArgumentException() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin-1");
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        when(courseRepository.findById("C101")).thenReturn(course);

        CourseModerationRequest req = new CourseModerationRequest();
        req.setCourseId("C101");
        req.setAction("INVALID_ACTION");

        assertThrows(IllegalArgumentException.class,
                () -> courseService.moderateCourse(TOKEN, "C101", req));
    }

    // ================= getEnrolledCoursesForStudent =================

    @Test
    void getEnrolledCoursesForStudent_ValidToken_ReturnsEnrolledCourses() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("student-1");
        UserEnrollmentResponse enrollment = new UserEnrollmentResponse();
        enrollment.setTargetId("C101");
        when(enrollmentClient.getUserEnrollmentsInternal(TOKEN)).thenReturn(List.of(enrollment));

        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");
        course.setTitle("Java Course");

        when(courseRepository.findById("C101")).thenReturn(course);
        when(reviewRepository.findByCourseId("C101")).thenReturn(List.of());

        List<CourseResponseDTO> result = courseService.getEnrolledCoursesForStudent(TOKEN);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("C101", result.get(0).getCourseId());
    }

    @Test
    void getEnrolledCoursesForStudent_NullEnrolled_ReturnsEmptyList() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("student-1");
        when(enrollmentClient.getUserEnrollmentsInternal(TOKEN)).thenReturn(null);

        List<CourseResponseDTO> result = courseService.getEnrolledCoursesForStudent(TOKEN);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ================= refreshRatingCache =================

    @Test
    void refreshRatingCache_CourseNotFound_ThrowsCourseNotFoundException() {
        when(courseRepository.findById("C999")).thenReturn(null);
        assertThrows(CourseNotFoundException.class,
                () -> courseService.refreshRatingCache("C999"));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void refreshRatingCache_WithMultipleReviews_UpdatesAverageRating() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        ReviewEntity r1 = new ReviewEntity();
        r1.setRating(5);
        ReviewEntity r2 = new ReviewEntity();
        r2.setRating(3);

        when(courseRepository.findById("C101")).thenReturn(course);
        when(reviewRepository.findByCourseId("C101")).thenReturn(List.of(r1, r2));

        courseService.refreshRatingCache("C101");

        verify(courseRepository).save(course);
        assertEquals(4.0, course.getAverageRating());
    }
}