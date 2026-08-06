package com.example.course_service.service;

import com.example.course_service.dto.request.AttachRecordingRequest;
import com.example.course_service.dto.request.LinkLiveSessionRequest;
import com.example.course_service.dto.response.LiveCourseDetailResponseDTO;
import com.example.course_service.dto.response.LiveSessionResponse;
import com.example.course_service.exception.CourseNotFoundException;
import com.example.course_service.feign.LiveClient;
import com.example.course_service.feign.TrainerClient;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.LiveCourseMapping;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LiveCourseMappingRepository;
import com.example.course_service.repository.ReviewRepository;
import com.example.course_service.service.impl.LiveCourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LiveCourseServiceImplTest {

    private LiveCourseServiceImpl liveCourseService;
    private CourseRepository courseRepository;
    private LiveCourseMappingRepository liveMappingRepository;
    private LiveClient liveClient;
    private TrainerClient trainerClient;
    private CategoryRepository categoryRepository;
    private ReviewRepository reviewRepository;
    private FileService fileService;

    @BeforeEach
    void setup() {
        courseRepository = mock(CourseRepository.class);
        liveMappingRepository = mock(LiveCourseMappingRepository.class);
        liveClient = mock(LiveClient.class);
        trainerClient = mock(TrainerClient.class);
        categoryRepository = mock(CategoryRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        fileService = mock(FileService.class);

        liveCourseService = new LiveCourseServiceImpl(
                courseRepository, liveMappingRepository, liveClient,
                trainerClient, categoryRepository, reviewRepository, fileService
        );
    }

    private CourseEntity buildLiveCourse() {
        CourseEntity course = new CourseEntity();
        course.setCourseId("course-1");
        course.setCourseType("LIVE");
        course.setCreatorId("user-1");
        course.setCreatorName("Trainer");
        return course;
    }

    // ================= LINK LIVE SESSION =================

    @Test
    void linkLiveSession_Success_NewMapping() {
        when(courseRepository.findById("course-1")).thenReturn(buildLiveCourse());
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);
        when(liveClient.getSessionById("session-1")).thenReturn(new LiveSessionResponse());

        LinkLiveSessionRequest request = new LinkLiveSessionRequest();
        request.setLiveSessionId("session-1");

        String result = liveCourseService.linkLiveSession("course-1", request);

        assertEquals("Live session linked successfully.", result);
        verify(liveMappingRepository).save(any(LiveCourseMapping.class));
    }

    @Test
    void linkLiveSession_Success_UpdateExistingMapping() {
        LiveCourseMapping existing = new LiveCourseMapping();
        existing.setId("map-1");
        existing.setCourseId("course-1");

        when(courseRepository.findById("course-1")).thenReturn(buildLiveCourse());
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(existing);
        when(liveClient.getSessionById("session-1")).thenReturn(new LiveSessionResponse());

        LinkLiveSessionRequest request = new LinkLiveSessionRequest();
        request.setLiveSessionId("session-1");

        String result = liveCourseService.linkLiveSession("course-1", request);

        assertEquals("Live session linked successfully.", result);
        verify(liveMappingRepository).save(existing);
    }

    @Test
    void linkLiveSession_NotLiveCourse_ThrowsException() {
        CourseEntity recorded = new CourseEntity();
        recorded.setCourseId("course-1");
        recorded.setCourseType("RECORDED");

        when(courseRepository.findById("course-1")).thenReturn(recorded);

        LinkLiveSessionRequest request = new LinkLiveSessionRequest();
        request.setLiveSessionId("session-1");

        assertThrows(IllegalArgumentException.class,
                () -> liveCourseService.linkLiveSession("course-1", request));
    }

    @Test
    void linkLiveSession_NullSessionId_ThrowsException() {
        when(courseRepository.findById("course-1")).thenReturn(buildLiveCourse());

        LinkLiveSessionRequest request = new LinkLiveSessionRequest();
        request.setLiveSessionId(null);

        assertThrows(IllegalArgumentException.class,
                () -> liveCourseService.linkLiveSession("course-1", request));
    }

    @Test
    void linkLiveSession_CourseNotFound_ThrowsException() {
        when(courseRepository.findById("course-1")).thenReturn(null);

        LinkLiveSessionRequest request = new LinkLiveSessionRequest();
        request.setLiveSessionId("session-1");

        assertThrows(CourseNotFoundException.class,
                () -> liveCourseService.linkLiveSession("course-1", request));
    }

    // ================= ATTACH RECORDING =================

    @Test
    void attachRecording_Success_NewMapping() {
        when(courseRepository.findById("course-1")).thenReturn(buildLiveCourse());
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);

        AttachRecordingRequest request = new AttachRecordingRequest();
        request.setCourseId("course-1");
        request.setLiveSessionId("session-1");
        request.setRecordingUrl("https://s3.com/recording.mp4");

        String result = liveCourseService.attachRecording(request);

        assertEquals("Recording attached successfully.", result);
        verify(liveMappingRepository).save(any(LiveCourseMapping.class));
    }

    @Test
    void attachRecording_NullFields_ThrowsException() {
        AttachRecordingRequest request = new AttachRecordingRequest();
        request.setCourseId(null);

        assertThrows(IllegalArgumentException.class,
                () -> liveCourseService.attachRecording(request));
    }

    @Test
    void attachRecording_NotLiveCourse_ThrowsException() {
        CourseEntity recorded = new CourseEntity();
        recorded.setCourseId("course-1");
        recorded.setCourseType("RECORDED");

        when(courseRepository.findById("course-1")).thenReturn(recorded);

        AttachRecordingRequest request = new AttachRecordingRequest();
        request.setCourseId("course-1");
        request.setLiveSessionId("session-1");
        request.setRecordingUrl("https://s3.com/recording.mp4");

        assertThrows(IllegalArgumentException.class,
                () -> liveCourseService.attachRecording(request));
    }

    // ================= GET LIVE COURSE DETAIL =================

    @Test
    void getLiveCourseDetail_Success() {
        CourseEntity course = buildLiveCourse();
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        LiveCourseMapping mapping = new LiveCourseMapping();
        mapping.setLiveSessionId("session-1");
        mapping.setRecordingUrl("https://s3.com/recording.mp4");

        LiveSessionResponse session = new LiveSessionResponse();
        session.setSessionId("session-1");

        when(courseRepository.findById("course-1")).thenReturn(course);
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(mapping);
        when(liveClient.getSessionById("session-1")).thenReturn(session);
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of());
        when(fileService.generatePresignedUrl(any())).thenReturn("https://presigned.url");

        LiveCourseDetailResponseDTO result = liveCourseService.getLiveCourseDetail("course-1");

        assertNotNull(result);
        assertEquals("course-1", result.getCourseId());
        assertEquals("session-1", result.getLiveSessionId());
    }

    @Test
    void getLiveCourseDetail_NotLiveCourse_ThrowsException() {
        CourseEntity recorded = new CourseEntity();
        recorded.setCourseId("course-1");
        recorded.setCourseType("RECORDED");

        when(courseRepository.findById("course-1")).thenReturn(recorded);

        assertThrows(IllegalArgumentException.class,
                () -> liveCourseService.getLiveCourseDetail("course-1"));
    }

    @Test
    void getLiveCourseDetail_NoMapping_ReturnsNullSessionFields() {
        CourseEntity course = buildLiveCourse();
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        when(courseRepository.findById("course-1")).thenReturn(course);
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of());
        when(fileService.generatePresignedUrl(any())).thenReturn("url");

        LiveCourseDetailResponseDTO result = liveCourseService.getLiveCourseDetail("course-1");

        assertNotNull(result);
        assertNull(result.getLiveSessionId());
    }

    @Test
    void getLiveCourseDetail_LiveClientThrows_ReturnsNullSession() {
        CourseEntity course = buildLiveCourse();
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        LiveCourseMapping mapping = new LiveCourseMapping();
        mapping.setLiveSessionId("session-1");

        when(courseRepository.findById("course-1")).thenReturn(course);
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(mapping);
        when(liveClient.getSessionById("session-1")).thenThrow(new RuntimeException("Feign error"));
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of());
        when(fileService.generatePresignedUrl(any())).thenReturn("url");

        LiveCourseDetailResponseDTO result = liveCourseService.getLiveCourseDetail("course-1");

        assertNotNull(result);
        assertNull(result.getLiveSessionId());
    }

    @Test
    void getLiveCourseDetail_TrainerClientThrows_UsesCreatorName() {
        CourseEntity course = buildLiveCourse();
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        when(courseRepository.findById("course-1")).thenReturn(course);
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);
        when(trainerClient.getTrainerById(any())).thenThrow(new RuntimeException("Feign error"));
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of());
        when(fileService.generatePresignedUrl(any())).thenReturn("url");

        LiveCourseDetailResponseDTO result = liveCourseService.getLiveCourseDetail("course-1");

        assertNotNull(result);
        assertEquals("Trainer", result.getInstructorName());
    }

    @Test
    void getLiveCourseDetail_WithTrainerData_UsesTrainerFields() {
        CourseEntity course = buildLiveCourse();
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        com.example.course_service.dto.response.TrainerResponseDTO trainer =
                new com.example.course_service.dto.response.TrainerResponseDTO();
        trainer.setFullName("John Doe");
        trainer.setDesignation("Senior Dev");
        trainer.setTrainingSpecialization("Java");
        trainer.setProfilePictureURL("https://photo.jpg");

        when(courseRepository.findById("course-1")).thenReturn(course);
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);
        when(trainerClient.getTrainerById("user-1")).thenReturn(trainer);
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of());
        when(fileService.generatePresignedUrl(any())).thenReturn("url");

        LiveCourseDetailResponseDTO result = liveCourseService.getLiveCourseDetail("course-1");

        assertEquals("John Doe", result.getInstructorName());
        assertEquals("Senior Dev", result.getInstructorTitle());
    }

    @Test
    void getLiveCourseDetail_WithCategory_ReturnsCategoryName() {
        CourseEntity course = buildLiveCourse();
        course.setCategoryId("cat-1");
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        com.example.course_service.model.CategoryEntity category =
                new com.example.course_service.model.CategoryEntity();
        category.setName("Programming");

        when(courseRepository.findById("course-1")).thenReturn(course);
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);
        when(categoryRepository.findById("cat-1")).thenReturn(category);
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of());
        when(fileService.generatePresignedUrl(any())).thenReturn("url");

        LiveCourseDetailResponseDTO result = liveCourseService.getLiveCourseDetail("course-1");

        assertEquals("Programming", result.getCategoryName());
    }

    @Test
    void getLiveCourseDetail_CategoryThrows_ReturnsNullCategoryName() {
        CourseEntity course = buildLiveCourse();
        course.setCategoryId("cat-1");
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        when(courseRepository.findById("course-1")).thenReturn(course);
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);
        when(categoryRepository.findById("cat-1")).thenThrow(new RuntimeException("DB error"));
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of());
        when(fileService.generatePresignedUrl(any())).thenReturn("url");

        LiveCourseDetailResponseDTO result = liveCourseService.getLiveCourseDetail("course-1");

        assertNull(result.getCategoryName());
    }

    @Test
    void getLiveCourseDetail_WithReviews_ReturnsSortedReviews() {
        CourseEntity course = buildLiveCourse();
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        com.example.course_service.model.ReviewEntity r1 = new com.example.course_service.model.ReviewEntity();
        r1.setReviewId("r1");
        r1.setCourseId("course-1");
        r1.setCreatedAt(Instant.ofEpochMilli(1000));

        com.example.course_service.model.ReviewEntity r2 = new com.example.course_service.model.ReviewEntity();
        r2.setReviewId("r2");
        r2.setCourseId("course-1");
        r2.setCreatedAt(Instant.ofEpochMilli(2000));

        when(courseRepository.findById("course-1")).thenReturn(course);
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of(r1, r2));
        when(fileService.generatePresignedUrl(any())).thenReturn("url");

        LiveCourseDetailResponseDTO result = liveCourseService.getLiveCourseDetail("course-1");

        assertEquals(2, result.getTotalReviews());
        assertEquals("r2", result.getReviews().get(0).getReviewId()); // sorted desc
    }

    @Test
    void getLiveCourseDetail_ReviewWithNullDate_HandledGracefully() {
        CourseEntity course = buildLiveCourse();
        course.setThumbnailKey("thumb.jpg");
        course.setDemoVideoKey("demo.mp4");

        com.example.course_service.model.ReviewEntity r = new com.example.course_service.model.ReviewEntity();
        r.setReviewId("r1");
        r.setCreatedAt(null); // null date

        when(courseRepository.findById("course-1")).thenReturn(course);
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of(r));
        when(fileService.generatePresignedUrl(any())).thenReturn("url");

        assertDoesNotThrow(() -> liveCourseService.getLiveCourseDetail("course-1"));
    }

    @Test
    void linkLiveSession_EmptySessionId_ThrowsException() {
        when(courseRepository.findById("course-1")).thenReturn(buildLiveCourse());

        LinkLiveSessionRequest request = new LinkLiveSessionRequest();
        request.setLiveSessionId("   ");

        assertThrows(IllegalArgumentException.class,
                () -> liveCourseService.linkLiveSession("course-1", request));
    }

    @Test
    void linkLiveSession_LiveClientThrows_ThrowsIllegalArgument() {
        when(courseRepository.findById("course-1")).thenReturn(buildLiveCourse());
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(null);
        when(liveClient.getSessionById("bad-session")).thenThrow(new RuntimeException("Not found"));

        LinkLiveSessionRequest request = new LinkLiveSessionRequest();
        request.setLiveSessionId("bad-session");

        assertThrows(IllegalArgumentException.class,
                () -> liveCourseService.linkLiveSession("course-1", request));
    }

    @Test
    void attachRecording_UpdateExistingMapping() {
        LiveCourseMapping existing = new LiveCourseMapping();
        existing.setId("map-1");
        existing.setCourseId("course-1");

        when(courseRepository.findById("course-1")).thenReturn(buildLiveCourse());
        when(liveMappingRepository.findByCourseId("course-1")).thenReturn(existing);

        AttachRecordingRequest request = new AttachRecordingRequest();
        request.setCourseId("course-1");
        request.setLiveSessionId("session-1");
        request.setRecordingUrl("https://s3.com/recording.mp4");

        String result = liveCourseService.attachRecording(request);

        assertEquals("Recording attached successfully.", result);
        verify(liveMappingRepository).save(existing);
        assertEquals("session-1", existing.getLiveSessionId());
    }
}
