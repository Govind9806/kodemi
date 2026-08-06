package com.example.course_service.service;

import com.example.course_service.dto.request.*;
import com.example.course_service.dto.response.*;
import com.example.course_service.exception.*;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.feign.TrainerClient;
import com.example.course_service.model.*;
import com.example.course_service.repository.*;
import com.example.course_service.service.impl.CourseServiceImpl;
import com.example.course_service.service.notification.NotificationPublisher;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseServiceImplCoverageTest {

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
    private ObjectProvider<CourseServiceImpl> selfProvider;

    private CourseServiceImpl courseService;
    private static final String TOKEN = "Bearer test-token";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

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
        selfProvider = mock(ObjectProvider.class);

        courseService = new CourseServiceImpl(
                courseRepository, categoryRepository, moduleRepository, lessonRepository,
                reviewRepository, trainerClient, enrollmentClient, fileService,
                uploadService, notificationPublisher, jwtUtil, selfProvider
        );
        when(selfProvider.getIfAvailable(any())).thenReturn(courseService);
    }

    private CourseEntity createBaseCourse(String id) {
        CourseEntity c = new CourseEntity();
        c.setCourseId(id);
        c.setTitle("Test Course " + id);
        c.setCreatorId("trainer-1");
        c.setStatus("UNVERIFIED");
        c.setIsVerified(false);
        c.setCourseType("RECORDED");
        c.setCategoryId("cat-1");
        c.setThumbnailKey("thumb.jpg");
        c.setDemoVideoKey("demo.mp4");
        return c;
    }

    @Test
    void createCourse_NullRequest_ThrowsNullException() {
        assertThrows(NullException.class, () -> courseService.createCourse(TOKEN, null, null, null));
    }

    @Test
    void createCourse_NullToken_ThrowsTokenNotFoundException() {
        CourseEntity req = new CourseEntity();
        assertThrows(TokenNotFoundException.class, () -> courseService.createCourse(null, req, null, null));
    }

    @Test
    void createCourse_MissingThumbnailAndKey_ThrowsThumbnailNotFoundException() {
        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("trainer-1");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        CourseEntity req = new CourseEntity();
        req.setThumbnailKey(null);

        assertThrows(ThumbnailNotFoundException.class, () -> courseService.createCourse(TOKEN, req, null, null));
    }

    @Test
    void createCourse_MissingDemoVideoAndKey_ThrowsIllegalArgumentException() {
        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("trainer-1");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        CourseEntity req = new CourseEntity();
        req.setThumbnailKey("thumb.jpg");
        req.setDemoVideoKey(null);

        assertThrows(IllegalArgumentException.class, () -> courseService.createCourse(TOKEN, req, null, null));
    }

    @Test
    void createCourse_WithMultipartFiles_UploadsDirectly() {
        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("trainer-1");
        trainer.setFullName("John Doe");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        CategoryEntity cat = new CategoryEntity();
        cat.setName("Backend");
        when(categoryRepository.findById(anyString())).thenReturn(cat);

        MockMultipartFile thumb = new MockMultipartFile("thumbnail", "thumb.png", "image/png", "data".getBytes());
        MockMultipartFile demo = new MockMultipartFile("demoVideo", "demo.mp4", "video/mp4", "data".getBytes());

        when(uploadService.uploadFileDirectly(eq(TOKEN), eq(thumb), eq(FileType.THUMBNAIL), anyString())).thenReturn("s3-thumb");
        when(uploadService.uploadFileDirectly(eq(TOKEN), eq(demo), eq(FileType.DEMO_VIDEO), anyString())).thenReturn("s3-demo");

        CourseEntity req = new CourseEntity();
        req.setTitle("Java Masterclass");
        req.setCategoryId("cat-1");

        Map<String, Object> result = courseService.createCourse(TOKEN, req, demo, thumb);
        assertNotNull(result.get("courseId"));
        verify(courseRepository).save(any(CourseEntity.class));
    }

    @Test
    void updateCourse_NullRequest_ThrowsNullException() {
        assertThrows(NullException.class, () -> courseService.updateCourse(TOKEN, "C1", null, null));
    }

    @Test
    void updateCourse_NotOwner_ThrowsForbiddenException() {
        CourseEntity existing = createBaseCourse("C1");
        when(courseRepository.findById("C1")).thenReturn(existing);

        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("trainer-other");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        CourseEntity updateReq = new CourseEntity();
        assertThrows(ForbiddenException.class, () -> courseService.updateCourse(TOKEN, "C1", updateReq, null));
    }

    @Test
    void updateCourse_AllFieldsAndThumbnail_Success() {
        CourseEntity existing = createBaseCourse("C1");
        when(courseRepository.findById("C1")).thenReturn(existing);

        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("trainer-1");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        CategoryEntity cat = new CategoryEntity();
        cat.setName("Frontend");
        when(categoryRepository.findById("cat-2")).thenReturn(cat);

        MockMultipartFile newThumb = new MockMultipartFile("thumbnail", "new.png", "image/png", "bytes".getBytes());
        when(uploadService.uploadFileDirectly(TOKEN, newThumb, FileType.THUMBNAIL, "C1")).thenReturn("new-s3-thumb");

        CourseEntity req = new CourseEntity();
        req.setTitle("Updated Title");
        req.setDescription("Updated Desc");
        req.setCategoryId("cat-2");
        req.setLanguage("Spanish");
        req.setSkillLevel("Advanced");
        req.setPrice(149.99);
        req.setWelcomeMessage("Welcome!");
        req.setCategory(Set.of("Programming"));
        req.setSubCategory(Set.of("Java"));
        req.setTopic(Set.of("Spring Boot"));

        String res = courseService.updateCourse(TOKEN, "C1", req, newThumb);
        assertEquals("Course Updated Successfully.", res);
        assertEquals("new-s3-thumb", existing.getThumbnailKey());
        assertEquals("Updated Title", existing.getTitle());
    }

    @Test
    void updateCourse_ThumbnailKeyChanged_ValidatesAndConsumesKey() {
        CourseEntity existing = createBaseCourse("C1");
        existing.setThumbnailKey("old-key");
        when(courseRepository.findById("C1")).thenReturn(existing);

        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("trainer-1");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        CourseEntity req = new CourseEntity();
        req.setThumbnailKey("new-pre-uploaded-key");

        courseService.updateCourse(TOKEN, "C1", req, null);
        verify(uploadService).validateAndConsumeUpload("new-pre-uploaded-key", "trainer-1", "C1");
        assertEquals("new-pre-uploaded-key", existing.getThumbnailKey());
    }

    @Test
    void moderateCourse_NullOrBlankAction_ThrowsIllegalArgumentException() {
        CourseModerationRequest req = new CourseModerationRequest();
        req.setAction("   ");

        assertThrows(IllegalArgumentException.class, () -> courseService.moderateCourse(TOKEN, "C1", req));
        assertThrows(IllegalArgumentException.class, () -> courseService.moderateCourse(TOKEN, "C1", null));
    }

    @Test
    void moderateCourse_InvalidAction_ThrowsIllegalArgumentException() {
        CourseModerationRequest req = new CourseModerationRequest();
        req.setAction("INVALID_ACTION");
        assertThrows(IllegalArgumentException.class, () -> courseService.moderateCourse(TOKEN, "C1", req));
    }

    @Test
    void getAllReviewedCourses_ReturnsVerifiedAndRejected() {
        CourseEntity c1 = createBaseCourse("C1");
        c1.setStatus("VERIFIED");
        CourseEntity c2 = createBaseCourse("C2");
        c2.setStatus("REJECTED");
        CourseEntity c3 = createBaseCourse("C3");
        c3.setStatus("UNVERIFIED");

        when(courseRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        List<CourseResponseDTO> res = courseService.getAllReviewedCourses(TOKEN);
        assertEquals(2, res.size());
    }

    @Test
    void refreshCourseStats_FormatsMinutesCorrectly() {
        CourseEntity c = createBaseCourse("C1");
        when(courseRepository.findById("C1")).thenReturn(c);

        ModuleEntity m1 = new ModuleEntity();
        m1.setModuleId("M1");
        m1.setCourseId("C1");
        when(moduleRepository.findByCourseId("C1")).thenReturn(List.of(m1));

        LessonEntity l1 = new LessonEntity();
        l1.setDuration(90);
        LessonEntity l2 = new LessonEntity();
        l2.setDuration(30);
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(l1, l2));

        courseService.refreshCourseStats("C1");
        assertEquals("2 Hours", c.getDurationLabel());
        assertEquals(2, c.getLessonCount());
    }

    @Test
    void refreshCourseStats_SingleHourAndSingleMinuteFormatting() {
        CourseEntity c = createBaseCourse("C1");
        when(courseRepository.findById("C1")).thenReturn(c);

        ModuleEntity m1 = new ModuleEntity();
        m1.setModuleId("M1");
        when(moduleRepository.findByCourseId("C1")).thenReturn(List.of(m1));

        LessonEntity l1 = new LessonEntity();
        l1.setDuration(60);
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(l1));

        courseService.refreshCourseStats("C1");
        assertEquals("1 Hour", c.getDurationLabel());
    }

    @Test
    void refreshCourseStats_MinutesOnlyFormatting() {
        CourseEntity c = createBaseCourse("C1");
        when(courseRepository.findById("C1")).thenReturn(c);

        ModuleEntity m1 = new ModuleEntity();
        m1.setModuleId("M1");
        when(moduleRepository.findByCourseId("C1")).thenReturn(List.of(m1));

        LessonEntity l1 = new LessonEntity();
        l1.setDuration(15);
        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(l1));

        courseService.refreshCourseStats("C1");
        assertEquals("15 Minutes", c.getDurationLabel());
    }

    @Test
    void getEnrolledCoursesForStudent_NullOrBlankToken_ReturnsEmptyList() {
        assertTrue(courseService.getEnrolledCoursesForStudent(null).isEmpty());
        assertTrue(courseService.getEnrolledCoursesForStudent("   ").isEmpty());
    }

    @Test
    void getEnrolledCoursesForStudent_InactiveOrNullStatus_FiltersOut() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("student-1");

        UserEnrollmentResponse active = new UserEnrollmentResponse();
        active.setTargetId("C1");
        active.setStatus("ACTIVE");

        UserEnrollmentResponse inactive = new UserEnrollmentResponse();
        inactive.setTargetId("C2");
        inactive.setStatus("EXPIRED");

        UserEnrollmentResponse nullTarget = new UserEnrollmentResponse();
        nullTarget.setTargetId(null);
        nullTarget.setStatus("ACTIVE");

        when(enrollmentClient.getUserEnrollmentsInternal(TOKEN)).thenReturn(List.of(active, inactive, nullTarget));

        CourseEntity c1 = createBaseCourse("C1");
        when(courseRepository.findById("C1")).thenReturn(c1);

        List<CourseResponseDTO> result = courseService.getEnrolledCoursesForStudent(TOKEN);
        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).getCourseId());
    }

    @Test
    void getEnrolledCoursesForStudent_FeignThrows_ReturnsEmptyList() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("student-1");
        when(enrollmentClient.getUserEnrollmentsInternal(TOKEN)).thenThrow(new RuntimeException("Feign error"));

        List<CourseResponseDTO> result = courseService.getEnrolledCoursesForStudent(TOKEN);
        assertTrue(result.isEmpty());
    }

    @Test
    void demoVideoMultipartUpload_DelegatesToUploadService() {
        MultipartUploadInitRequest initReq = new MultipartUploadInitRequest();
        initReq.setFileName("video.mp4");
        initReq.setContentType("video/mp4");
        initReq.setFileSize(1000L);

        UploadInitResponse initRes = new UploadInitResponse("up-1", "key-1", "Initialized");
        when(uploadService.initiateMultipartUpload(eq(TOKEN), any(UploadInitRequest.class))).thenReturn(initRes);

        MultipartUploadInitResponse initResult = courseService.initiateDemoVideoMultipartUpload(TOKEN, initReq);
        assertEquals("up-1", initResult.getUploadId());
        assertEquals("key-1", initResult.getFileKey());

        when(uploadService.generatePresignedUrl(eq(TOKEN), any(UploadPresignedUrlRequest.class))).thenReturn("https://presigned.url");
        String presignedUrl = courseService.generateDemoVideoPresignedUrl(TOKEN, "up-1", "key-1", 1);
        assertEquals("https://presigned.url", presignedUrl);

        CompleteMultipartUploadRequestDTO completeReq = new CompleteMultipartUploadRequestDTO();
        completeReq.setUploadId("up-1");
        CompleteMultipartUploadRequestDTO.PartETag part = new CompleteMultipartUploadRequestDTO.PartETag();
        part.setPartNumber(1);
        part.setETag("etag1");
        completeReq.setParts(List.of(part));

        CompleteMultipartUploadResponse completeRes = new CompleteMultipartUploadResponse("up-1", "key-1", "Completed");
        when(uploadService.completeMultipartUpload(eq(TOKEN), any(UploadCompleteRequest.class))).thenReturn(completeRes);

        CompleteMultipartUploadResponse completeResult = courseService.completeDemoVideoMultipartUpload(TOKEN, completeReq);
        assertEquals("up-1", completeResult.getUploadId());

        AbortMultipartUploadRequestDTO abortReq = new AbortMultipartUploadRequestDTO();
        abortReq.setUploadId("up-1");
        String abortResult = courseService.abortDemoVideoMultipartUpload(TOKEN, abortReq);
        assertEquals("Upload aborted", abortResult);
    }

    @Test
    void createLiveCourse_Success() {
        TrainerResponseDTO trainer = new TrainerResponseDTO();
        trainer.setUserId("trainer-1");
        trainer.setFullName("John Doe");
        when(trainerClient.getTrainer(TOKEN)).thenReturn(trainer);

        CourseEntity req = new CourseEntity();
        req.setTitle("Live Java Course");
        req.setThumbnailKey("thumb.jpg");
        req.setDemoVideoKey("demo.mp4");

        Map<String, Object> result = courseService.createLiveCourse(TOKEN, req, null, null);
        assertEquals("Live Course Created Successfully", result.get("message"));
    }

    @Test
    void getCoursesByCreatorId_Overloads_FilterCorrectly() {
        CourseEntity c1 = createBaseCourse("C1");
        c1.setCreatorId("trainer-1");
        c1.setCourseType("RECORDED");

        CourseEntity c2 = createBaseCourse("C2");
        c2.setCreatorId("trainer-1");
        c2.setCourseType("LIVE");

        CourseEntity c3 = createBaseCourse("C3");
        c3.setCreatorId("trainer-2");
        c3.setCourseType("RECORDED");

        when(courseRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        List<CourseResponseDTO> allTrainer1 = courseService.getCoursesByCreatorId("trainer-1");
        assertEquals(2, allTrainer1.size());

        List<CourseResponseDTO> liveTrainer1 = courseService.getCoursesByCreatorId("trainer-1", "LIVE");
        assertEquals(1, liveTrainer1.size());
        assertEquals("C2", liveTrainer1.get(0).getCourseId());
    }

    @Test
    void getCourseDetail_PopulatesCategoryReviewsAndEnrolledCount() {
        CourseEntity c = createBaseCourse("C1");
        c.setCategoryId("cat-1");
        c.setThumbnailKey("thumb.png");
        c.setDemoVideoKey("demo.mp4");
        when(courseRepository.findById("C1")).thenReturn(c);

        CategoryEntity cat = new CategoryEntity();
        cat.setName("Computer Science");
        when(categoryRepository.findById("cat-1")).thenReturn(cat);

        ReviewEntity r1 = new ReviewEntity();
        r1.setReviewId("R1");
        r1.setCourseId("C1");
        r1.setCreatedAt(FIXED_INSTANT);

        ReviewEntity r2 = new ReviewEntity();
        r2.setReviewId("R2");
        r2.setCourseId("C1");
        r2.setCreatedAt(null);

        when(reviewRepository.findByCourseId("C1")).thenReturn(List.of(r1, r2));

        when(enrollmentClient.getEnrolledLearners("C1")).thenReturn(List.of("user-1"));
        when(fileService.generateDownloadUrl(anyString())).thenReturn("https://download.url");

        CourseResponseDTO dto = courseService.getCourseDetail("C1");
        assertNotNull(dto);
        assertEquals("Computer Science", dto.getCategoryName());
        assertEquals(2, dto.getTotalReviews());
        assertEquals(1, dto.getEnrolledCount());
        assertEquals("https://download.url", dto.getThumbnailUrl());
        assertEquals("https://download.url", dto.getDemoVideoUrl());
    }

    @Test
    void getCourseDetail_CategoryAndEnrollmentException_HandledSilently() {
        CourseEntity c = createBaseCourse("C1");
        c.setCategoryId("cat-err");
        c.setThumbnailKey(null);
        c.setDemoVideoKey(null);
        when(courseRepository.findById("C1")).thenReturn(c);

        when(categoryRepository.findById("cat-err")).thenThrow(new RuntimeException("DB error"));
        when(enrollmentClient.getEnrolledLearners("C1")).thenThrow(new RuntimeException("Feign error"));

        CourseResponseDTO dto = courseService.getCourseDetail("C1");
        assertNotNull(dto);
        assertNull(dto.getCategoryName());
        assertEquals(0, dto.getEnrolledCount());
        assertNull(dto.getThumbnailUrl());
        assertNull(dto.getDemoVideoUrl());
    }

    @Test
    void getCoursesByCategory_ReturnsDTOList() {
        CourseEntity c1 = createBaseCourse("C1");
        when(courseRepository.findByCategoryId("cat-1")).thenReturn(List.of(c1));

        List<CourseResponseDTO> dtos = courseService.getCoursesByCategory("cat-1");
        assertEquals(1, dtos.size());
        assertEquals("C1", dtos.get(0).getCourseId());
    }

    @Test
    void refreshRatingCache_CalculatesAverageWithNullRatingsAndEmptyList() {
        CourseEntity c = createBaseCourse("C1");
        when(courseRepository.findById("C1")).thenReturn(c);

        when(reviewRepository.findByCourseId("C1")).thenReturn(List.of());
        courseService.refreshRatingCache("C1");
        verify(courseRepository, never()).save(any());

        ReviewEntity r1 = new ReviewEntity();
        r1.setRating(5);
        ReviewEntity r2 = new ReviewEntity();
        r2.setRating(null);

        when(reviewRepository.findByCourseId("C1")).thenReturn(List.of(r1, r2));
        courseService.refreshRatingCache("C1");

        assertEquals(2.5, c.getAverageRating());
        assertEquals(2, c.getTotalReviews());
        verify(courseRepository).save(c);
    }

    @Test
    void toDTO_TrainerClientThrows_HandledGracefully() {
        CourseEntity c = createBaseCourse("C1");
        c.setCreatorId("trainer-err");
        c.setCreatorName("Default Name");
        when(courseRepository.findAll()).thenReturn(List.of(c));

        when(trainerClient.getTrainerById("trainer-err")).thenThrow(new RuntimeException("Trainer service down"));

        List<CourseResponseDTO> dtos = courseService.getAllCoursesForAdmin();
        assertEquals(1, dtos.size());
        assertEquals("Default Name", dtos.get(0).getInstructorName());
    }

    @Test
    void getModulesWithLessonsForCourse_SortsAndPopulatesLessons() {
        CourseEntity c = createBaseCourse("C1");
        when(courseRepository.findAll()).thenReturn(List.of(c));

        ModuleEntity m2 = new ModuleEntity();
        m2.setModuleId("M2");
        m2.setCourseId("C1");
        m2.setOrderIndex(2);

        ModuleEntity m1 = new ModuleEntity();
        m1.setModuleId("M1");
        m1.setCourseId("C1");
        m1.setOrderIndex(null);

        when(moduleRepository.findByCourseId("C1")).thenReturn(List.of(m2, m1));

        LessonEntity l1 = new LessonEntity();
        l1.setLessonId("L1");
        l1.setModuleId("M1");
        l1.setOrderIndex(null);
        l1.setVideoKey("video.mp4");

        when(lessonRepository.findByModuleId("M1")).thenReturn(List.of(l1));
        when(lessonRepository.findByModuleId("M2")).thenReturn(List.of());
        when(fileService.generateDownloadUrl("video.mp4")).thenReturn("https://video.url");

        List<CourseResponseDTO> dtos = courseService.getAllCoursesForAdmin();
        assertEquals(1, dtos.size());
        List<ModuleResponseDTO> modules = dtos.get(0).getModules();
        assertEquals(2, modules.size());
        assertEquals("M1", modules.get(0).getModuleId());
        assertEquals("https://video.url", modules.get(0).getLessons().get(0).getVideoUrl());
    }

    @Test
    void verifyCourse_And_RejectCourse_WithRemarksVariants() {
        CourseEntity c1 = createBaseCourse("C1");
        when(courseRepository.findById("C1")).thenReturn(c1);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin-1");

        String res1 = courseService.verifyCourse(TOKEN, "C1");
        assertEquals("Course Verified Successfully.", res1);
        assertTrue(c1.getIsVerified());
        assertEquals("VERIFIED", c1.getStatus());

        String res2 = courseService.rejectCourse(TOKEN, "C1", "Not detailed enough");
        assertEquals("Course Rejected.", res2);
        assertFalse(c1.getIsVerified());
        assertEquals("REJECTED", c1.getStatus());

        courseService.rejectCourse(TOKEN, "C1", "  ");
        courseService.rejectCourse(TOKEN, "C1", null);
    }

    @Test
    void publishNotification_PublisherThrowsException_CaughtSilently() {
        CourseEntity c = createBaseCourse("C1");
        when(courseRepository.findById("C1")).thenReturn(c);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin-1");

        doThrow(new RuntimeException("Notification service down")).when(notificationPublisher).publish(any());

        assertDoesNotThrow(() -> courseService.verifyCourse(TOKEN, "C1"));
    }

    @Test
    void getAllCourses_FiltersVerifiedOnly() {
        CourseEntity verified = createBaseCourse("C1");
        verified.setIsVerified(true);

        CourseEntity unverified = createBaseCourse("C2");
        unverified.setIsVerified(false);

        CourseEntity nullVerified = createBaseCourse("C3");
        nullVerified.setIsVerified(null);

        when(courseRepository.findAll()).thenReturn(List.of(verified, unverified, nullVerified));

        List<CourseResponseDTO> dtos = courseService.getAllCourses();
        assertEquals(1, dtos.size());
        assertEquals("C1", dtos.get(0).getCourseId());
    }

    @Test
    void getAllCoursesForAdmin_HandlesNullCourses() {
        CourseEntity c1 = createBaseCourse("C1");

        List<CourseEntity> list = new java.util.ArrayList<>();
        list.add(c1);
        list.add(null);

        when(courseRepository.findAll()).thenReturn(list);

        List<CourseResponseDTO> dtos = courseService.getAllCoursesForAdmin();
        assertEquals(1, dtos.size());
        assertEquals("C1", dtos.get(0).getCourseId());
    }

    @Test
    void streamAllCoursesAdmin_HandlesSubscriberAndIOException() {
        CourseEntity c = createBaseCourse("C1");
        when(courseRepository.findAll()).thenReturn(List.of(c));
        when(courseRepository.findById("C1")).thenReturn(c);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("admin-1");

        SseEmitter emitter = courseService.streamAllCoursesAdmin();
        assertNotNull(emitter);

        courseService.verifyCourse(TOKEN, "C1");
    }
}