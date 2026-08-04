package com.example.course_service.service;

import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.TrainerResponseDTO;
import com.example.course_service.exception.CourseNotFoundException;
import com.example.course_service.exception.NullException;
import com.example.course_service.feign.TrainerClient;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.repository.*;
import com.example.course_service.service.impl.CourseServiceImpl;
import com.example.course_service.service.UploadService;
import com.example.course_service.service.notification.NotificationPublisher;
import com.example.course_service.model.FileType;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseServiceImplTest {

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
                lessonRepository, reviewRepository, trainerClient,
                enrollmentClient, fileService, uploadService, notificationPublisher, jwtUtil
        );
    }

    @Test
    void createCourse_Success() throws Exception {
        CourseEntity request = new CourseEntity();
        request.setTitle("Java");
        request.setDemoVideoKey("preview/demo-videos/existing-key.mp4");

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);
        when(thumbnail.getOriginalFilename()).thenReturn("img.png");
        when(thumbnail.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(thumbnail.getSize()).thenReturn(4L);

        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("user-1");
        trainer.setFullName("Govind");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        java.util.Map<String, Object> result = courseService.createCourse(TOKEN, request, null, thumbnail);

        assertTrue(result.get("message").toString().startsWith("Course Created Successfully"));
        verify(courseRepository).save(any(CourseEntity.class));
    }

    @Test
    void createCourse_NullRequest_ThrowsException() {
        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(false);

        assertThrows(NullException.class, () -> courseService.createCourse(TOKEN, null, null, thumbnail));
    }

    @Test
    void createCourse_EmptyThumbnail_ThrowsException() {
        CourseEntity request = new CourseEntity();
        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.isEmpty()).thenReturn(true);

        assertThrows(Exception.class, () -> courseService.createCourse(TOKEN, request, null, thumbnail));
    }

    @Test
    void getAllCourses_ReturnsList() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C1");
        course.setIsVerified(true);
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(fileService.generateDownloadUrl(any())).thenReturn("url");

        List<CourseResponseDTO> result = courseService.getAllCourses();

        assertEquals(1, result.size());
    }

    @Test
    void updateCourse_Success() {
        CourseEntity existing = new CourseEntity();
        existing.setCourseId("C101");
        existing.setCreatorId("user-1");
        when(courseRepository.findById("C101")).thenReturn(existing);

        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("user-1");
        when(trainerClient.getTrainer("user-1")).thenReturn(trainer);

        String result = courseService.updateCourse("user-1", "C101", new CourseEntity(), null);

        assertTrue(result.startsWith("Course Updated Successfully."));
        verify(courseRepository).save(existing);
    }

    @Test
    void updateCourse_NotOwner_ThrowsException() {
        CourseEntity existing = new CourseEntity();
        existing.setCreatorId("user-1");
        when(courseRepository.findById("C101")).thenReturn(existing);

        CourseEntity request = new CourseEntity();
        assertThrows(RuntimeException.class,
                () -> courseService.updateCourse("other-user", "C101", request, null));
    }

    @Test
    void updateCourse_NotFound() {
        when(courseRepository.findById("C101")).thenReturn(null);

        CourseEntity request = new CourseEntity();
        assertThrows(CourseNotFoundException.class,
                () -> courseService.updateCourse("user-1", "C101", request, null));
    }

    @Test
    void verifyCourse_Success() {
        CourseEntity course = new CourseEntity();
        when(courseRepository.findById("C101")).thenReturn(course);

        String result = courseService.verifyCourse(TOKEN, "C101");

        assertEquals("Course Verified Successfully.", result);
        verify(courseRepository).save(course);
    }

    @Test
    void rejectCourse_Success() {
        CourseEntity course = new CourseEntity();
        when(courseRepository.findById("C101")).thenReturn(course);

        String result = courseService.rejectCourse(TOKEN, "C101", "rejection reason");

        assertEquals("Course Rejected.", result);
        verify(courseRepository).save(course);
    }

    @Test
    void getCoursesByCategory_Success() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C1");
        when(courseRepository.findByCategoryId("cat-1")).thenReturn(List.of(course));
        when(fileService.generateDownloadUrl(any())).thenReturn("url");

        List<CourseResponseDTO> result = courseService.getCoursesByCategory("cat-1");

        assertEquals(1, result.size());
    }

    @Test
    void streamAllCoursesAdmin_ReturnsEmitter() {
        CourseEntity unverified = new CourseEntity();
        unverified.setCourseId("C2");
        unverified.setIsVerified(false);
        unverified.setStatus("UNVERIFIED");

        CourseEntity verified = new CourseEntity();
        verified.setCourseId("C3");
        verified.setIsVerified(true);

        when(courseRepository.findAll()).thenReturn(List.of(unverified, verified));

        org.springframework.web.servlet.mvc.method.annotation.SseEmitter result = courseService.streamAllCoursesAdmin();

        assertNotNull(result);
    }

    @Test
    void refreshRatingCache_UpdatesRating() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        com.example.course_service.model.ReviewEntity r1 = new com.example.course_service.model.ReviewEntity();
        r1.setRating(4);
        com.example.course_service.model.ReviewEntity r2 = new com.example.course_service.model.ReviewEntity();
        r2.setRating(5);

        when(courseRepository.findById("C101")).thenReturn(course);
        when(reviewRepository.findByCourseId("C101")).thenReturn(List.of(r1, r2));

        courseService.refreshRatingCache("C101");

        verify(courseRepository).save(course);
        assertEquals(4.5, course.getAverageRating());
    }

    @Test
    void refreshRatingCache_EmptyReviews_DoesNotSave() {
        CourseEntity course = new CourseEntity();
        when(courseRepository.findById("C101")).thenReturn(course);
        when(reviewRepository.findByCourseId("C101")).thenReturn(List.of());

        courseService.refreshRatingCache("C101");

        verify(courseRepository, never()).save(any());
    }

    @Test
    void refreshCourseStats_UpdatesStats() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        com.example.course_service.model.ModuleEntity module = new com.example.course_service.model.ModuleEntity();
        module.setModuleId("M1");

        com.example.course_service.model.LessonEntity lesson = new com.example.course_service.model.LessonEntity();
        lesson.setDuration(30);

        when(courseRepository.findById("C101")).thenReturn(course);
        when(moduleRepository.findByCourseId("C101")).thenReturn(List.of(module));
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(lesson));

        courseService.refreshCourseStats("C101");

        verify(courseRepository).save(course);
        assertEquals(1, course.getLessonCount());
        assertEquals("30 Minutes", course.getDurationLabel());
    }

    @ParameterizedTest
    @CsvSource({
        "30, 30 Minutes",
        "90, 1h 30m",
        "120, 2 Hours",
        "60, 1 Hour"
    })
    void refreshCourseStats_FormatsLabel(int duration, String expectedLabel) {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        com.example.course_service.model.ModuleEntity module = new com.example.course_service.model.ModuleEntity();
        module.setModuleId("M1");

        com.example.course_service.model.LessonEntity lesson = new com.example.course_service.model.LessonEntity();
        lesson.setDuration(duration);

        when(courseRepository.findById("C101")).thenReturn(course);
        when(moduleRepository.findByCourseId("C101")).thenReturn(List.of(module));
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(lesson));

        courseService.refreshCourseStats("C101");

        verify(courseRepository).save(course);
        assertEquals(expectedLabel, course.getDurationLabel());
    }

    @Test
    void refreshCourseStats_NullDuration() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");

        com.example.course_service.model.ModuleEntity module = new com.example.course_service.model.ModuleEntity();
        module.setModuleId("M1");

        com.example.course_service.model.LessonEntity lesson = new com.example.course_service.model.LessonEntity();
        lesson.setDuration(null);

        when(courseRepository.findById("C101")).thenReturn(course);
        when(moduleRepository.findByCourseId("C101")).thenReturn(List.of(module));
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(lesson));

        courseService.refreshCourseStats("C101");

        assertEquals("0 Minutes", course.getDurationLabel());
    }

    @Test
    void getCoursesByCreatorId_LiveCourse_Success() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C_LIVE");
        course.setCreatorId("user-1");
        course.setCourseType("LIVE");
        when(courseRepository.findAll()).thenReturn(List.of(course));

        com.example.course_service.model.ModuleEntity module1 = new com.example.course_service.model.ModuleEntity();
        module1.setModuleId("M1");
        module1.setCourseId("C_LIVE");
        module1.setOrderIndex(2);

        com.example.course_service.model.ModuleEntity module2 = new com.example.course_service.model.ModuleEntity();
        module2.setModuleId("M2");
        module2.setCourseId("C_LIVE");
        module2.setOrderIndex(1); // will sort before M1

        when(moduleRepository.findByCourseId("C_LIVE")).thenReturn(List.of(module1, module2));

        com.example.course_service.model.LessonEntity lesson = new com.example.course_service.model.LessonEntity();
        lesson.setLessonId("L1");
        lesson.setModuleId("M2");
        lesson.setOrderIndex(1);
        lesson.setVideoKey("v-key");
        when(lessonRepository.findByModuleId("M2")).thenReturn(List.of(lesson));
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of());

        List<CourseResponseDTO> result = courseService.getCoursesByCreatorId("user-1", "LIVE");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("C_LIVE", result.get(0).getCourseId());
        assertNotNull(result.get(0).getModules());
        assertEquals(2, result.get(0).getModules().size());
        // Verify M2 is first due to lower orderIndex
        assertEquals("M2", result.get(0).getModules().get(0).getModuleId());
    }

    @Test
    void getAllReviewedCourses_Success() {
        CourseEntity c1 = new CourseEntity();
        c1.setCourseId("C1");
        c1.setStatus("VERIFIED");

        CourseEntity c2 = new CourseEntity();
        c2.setCourseId("C2");
        c2.setStatus("REJECTED");

        CourseEntity c3 = new CourseEntity();
        c3.setCourseId("C3");
        c3.setStatus("UNVERIFIED");

        when(courseRepository.findAll()).thenReturn(List.of(c1, c2, c3));
        when(moduleRepository.findByCourseId(any())).thenReturn(List.of());

        List<CourseResponseDTO> result = courseService.getAllReviewedCourses(TOKEN);
        assertEquals(2, result.size());
    }

    @Test
    void moderateCourse_Verify_Success() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");
        when(courseRepository.findById("C101")).thenReturn(course);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin-1");

        com.example.course_service.dto.request.CourseModerationRequest req = new com.example.course_service.dto.request.CourseModerationRequest();
        req.setAction("VERIFY");

        String result = courseService.moderateCourse(TOKEN, "C101", req);
        assertEquals("Course Verified Successfully.", result);
    }

    @Test
    void moderateCourse_Reject_Success() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("C101");
        when(courseRepository.findById("C101")).thenReturn(course);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin-1");

        com.example.course_service.dto.request.CourseModerationRequest req = new com.example.course_service.dto.request.CourseModerationRequest();
        req.setAction("REJECT");
        req.setRemarks("Insufficient content");

        String result = courseService.moderateCourse(TOKEN, "C101", req);
        assertEquals("Course Rejected.", result);
    }

    @Test
    void moderateCourse_InvalidAction_ThrowsException() {
        com.example.course_service.dto.request.CourseModerationRequest req = new com.example.course_service.dto.request.CourseModerationRequest();
        req.setAction("INVALID");

        assertThrows(IllegalArgumentException.class, () -> courseService.moderateCourse(TOKEN, "C101", req));
        assertThrows(IllegalArgumentException.class, () -> courseService.moderateCourse(TOKEN, "C101", null));
    }
}
