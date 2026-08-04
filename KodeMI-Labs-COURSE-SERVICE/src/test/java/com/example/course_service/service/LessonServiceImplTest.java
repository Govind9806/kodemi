package com.example.course_service.service;

import com.example.course_service.dto.response.LessonResponseDTO;
import com.example.course_service.feign.LiveClient;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.model.ModuleEntity;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.ModuleRepository;
import com.example.course_service.service.impl.LessonServiceImpl;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.service.notification.NotificationPublisher;
import com.example.course_service.util.JwtUtil;
import com.example.course_service.service.CourseService;
import com.example.course_service.service.impl.VideoProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LessonServiceImplTest {

    private LessonRepository lessonRepository;
    private ModuleRepository moduleRepository;
    private CourseRepository courseRepository;
    private FileService fileService;
    private JwtUtil jwtUtil;
    private LiveClient liveClient;
    private EnrollmentClient enrollmentClient;
    private NotificationPublisher notificationPublisher;
    private CourseService courseService;
    private VideoProcessingService videoProcessingService;
    private LessonServiceImpl lessonService;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setup() {
        lessonRepository = mock(LessonRepository.class);
        moduleRepository = mock(ModuleRepository.class);
        courseRepository = mock(CourseRepository.class);
        fileService = mock(FileService.class);
        jwtUtil = mock(JwtUtil.class);
        liveClient = mock(LiveClient.class);
        enrollmentClient = mock(EnrollmentClient.class);
        notificationPublisher = mock(NotificationPublisher.class);
        courseService = mock(CourseService.class);
        videoProcessingService = mock(VideoProcessingService.class);

        lessonService = new LessonServiceImpl(
                lessonRepository, moduleRepository, courseRepository,
                fileService, jwtUtil, liveClient, enrollmentClient, notificationPublisher, courseService, videoProcessingService
        );
    }

    private CourseEntity buildCourse(String creatorId) {
        CourseEntity course = new CourseEntity();
        course.setCourseId("course-1");
        course.setCreatorId(creatorId);
        course.setCourseType("RECORDED");
        return course;
    }

    private ModuleEntity buildModule() {
        ModuleEntity module = new ModuleEntity();
        module.setModuleId("module-1");
        module.setCourseId("course-1");
        return module;
    }

    private LessonEntity buildLesson() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("lesson-1");
        lesson.setModuleId("module-1");
        lesson.setTitle("Intro");
        lesson.setDescription("Intro lesson");
        lesson.setOrderIndex(1);
        lesson.setCreatedAt(new Date());
        lesson.setUpdatedAt(new Date());
        return lesson;
    }

    // ================= CREATE =================

    @Test
    void createLesson_Success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(moduleRepository.findById("module-1")).thenReturn(buildModule());
        when(courseRepository.findById("course-1")).thenReturn(buildCourse("user-1"));

        LessonEntity request = new LessonEntity();
        request.setModuleId("module-1");
        request.setTitle("New Lesson");
        request.setOrderIndex(1);

        java.util.Map<String, Object> result = lessonService.createLesson(request, TOKEN);

        assertTrue(result.get("message").toString().startsWith("Lesson Created Successfully"));

        ArgumentCaptor<LessonEntity> captor = ArgumentCaptor.forClass(LessonEntity.class);
        verify(lessonRepository).save(captor.capture());

        LessonEntity saved = captor.getValue();
        assertNotNull(saved.getLessonId());
        assertEquals("module-1", saved.getModuleId());
        assertEquals("New Lesson", saved.getTitle());
    }

    @Test
    void createLesson_NullBody_ThrowsException() {
        assertThrows(NullPointerException.class, () -> lessonService.createLesson(null, TOKEN));
        verify(lessonRepository, never()).save(any());
    }

    // ================= GET BY ID =================

    @Test
    void getLessonById_ReturnsDTO() {
        when(lessonRepository.findById("lesson-1")).thenReturn(buildLesson());
        when(moduleRepository.findById("module-1")).thenReturn(buildModule());

        LessonResponseDTO dto = lessonService.getLessonById("lesson-1", TOKEN);

        assertNotNull(dto);
        assertEquals("lesson-1", dto.getLessonId());
        assertEquals("Intro", dto.getTitle());
    }

    @Test
    void getLessonById_NotFound_ThrowsException() {
        when(lessonRepository.findById("L999")).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> lessonService.getLessonById("L999", TOKEN));
    }

    // ================= GET BY MODULE =================

    @Test
    void getLessonsByModule_Success() {
        when(moduleRepository.findById("module-1")).thenReturn(buildModule());
        when(lessonRepository.findByModuleId("module-1")).thenReturn(List.of(buildLesson()));

        List<LessonResponseDTO> result = lessonService.getLessonsByModule("module-1", TOKEN);

        assertEquals(1, result.size());
        assertEquals("Intro", result.get(0).getTitle());
    }

    // ================= UPDATE =================

    @Test
    void updateLesson_Success() {
        LessonEntity existing = buildLesson();
        when(lessonRepository.findById("lesson-1")).thenReturn(existing);

        LessonEntity update = new LessonEntity();
        update.setTitle("Updated Title");
        update.setDescription("Updated Desc");

        String result = lessonService.updateLesson(TOKEN, "lesson-1", update);

        assertEquals("Lesson updated", result);
        verify(lessonRepository).save(existing);
        assertEquals("Updated Title", existing.getTitle());
    }

    @Test
    void updateLesson_NullBody_ThrowsException() {
        when(lessonRepository.findById("lesson-1")).thenReturn(buildLesson());

        assertThrows(NullPointerException.class,
                () -> lessonService.updateLesson(TOKEN, "lesson-1", null));
    }

    @Test
    void updateLesson_NotFound_ThrowsException() {
        when(lessonRepository.findById("L999")).thenReturn(null);

        LessonEntity request = new LessonEntity();
        assertThrows(NullPointerException.class,
                () -> lessonService.updateLesson(TOKEN, "L999", request));
    }

    // ================= GENERATE UPLOAD URL =================

    @Test
    void generateUploadUrl_ReturnsMap() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(fileService.generateUploadUrl(anyString(), anyString())).thenReturn("https://s3.com/upload");

        var result = lessonService.generateUploadUrl(TOKEN, "lesson-1", "video.mp4");

        assertNotNull(result);
        assertTrue(result.containsKey("uploadUrl"));
        assertEquals("https://s3.com/upload", result.get("uploadUrl"));
    }

    // ================= SAVE UPLOADED CONTENT =================

    @Test
    void saveUploadedContent_Success() {
        LessonEntity lesson = buildLesson();
        lesson.setContentKey(new java.util.ArrayList<>());
        when(lessonRepository.findById("lesson-1")).thenReturn(lesson);

        com.example.course_service.dto.request.SaveUploadedContentRequest request =
                new com.example.course_service.dto.request.SaveUploadedContentRequest();
        request.setLessonId("lesson-1");
        request.setFileKey("videos/test.mp4");
        request.setLabel("Lecture 1");

        String result = lessonService.saveUploadedContent(TOKEN, request);

        assertEquals("Content saved", result);
        verify(lessonRepository).save(lesson);
        assertEquals(1, lesson.getContentKey().size());
    }

    @Test
    void saveUploadedContent_NullContentKey_InitializesAndSaves() {
        LessonEntity lesson = buildLesson();
        lesson.setContentKey(null);
        when(lessonRepository.findById("lesson-1")).thenReturn(lesson);

        com.example.course_service.dto.request.SaveUploadedContentRequest request =
                new com.example.course_service.dto.request.SaveUploadedContentRequest();
        request.setLessonId("lesson-1");
        request.setFileKey("videos/test.mp4");

        String result = lessonService.saveUploadedContent(TOKEN, request);

        assertEquals("Content saved", result);
        assertNotNull(lesson.getContentKey());
    }

    // ================= GET DOWNLOAD URL =================

    @Test
    void getDownloadUrl_ReturnsUrl() {
        when(fileService.generateDownloadUrl("videos/test.mp4")).thenReturn("https://cdn.example.com/signed");

        String result = lessonService.getDownloadUrl(TOKEN, "lesson-1", "videos/test.mp4");

        assertEquals("https://cdn.example.com/signed", result);
    }

    // ================= SAVE LIVE RECORDING =================

    @Test
    void saveLiveRecording_Success() {
        LessonEntity lesson = buildLesson();
        lesson.setContentKey(new java.util.ArrayList<>());
        when(lessonRepository.findById("lesson-1")).thenReturn(lesson);

        com.example.course_service.dto.request.LiveRecordingRequest request =
                new com.example.course_service.dto.request.LiveRecordingRequest();
        request.setLessonId("lesson-1");
        request.setRecordingKey("recordings/session.mp4");
        request.setDuration(3600);

        String result = lessonService.saveLiveRecording(request);

        assertEquals("Recording saved", result);
        verify(lessonRepository).save(lesson);
        assertEquals(3600, lesson.getDuration());
    }

    @Test
    void saveLiveRecording_NullContentKey_InitializesAndSaves() {
        LessonEntity lesson = buildLesson();
        lesson.setContentKey(null);
        when(lessonRepository.findById("lesson-1")).thenReturn(lesson);

        com.example.course_service.dto.request.LiveRecordingRequest request =
                new com.example.course_service.dto.request.LiveRecordingRequest();
        request.setLessonId("lesson-1");
        request.setRecordingKey("recordings/session.mp4");
        request.setDuration(60);

        String result = lessonService.saveLiveRecording(request);

        assertEquals("Recording saved", result);
        assertNotNull(lesson.getContentKey());
    }

    // ================= CREATE LIVE LESSON =================

    @Test
    void createLesson_LiveType_InLiveCourse_Success() {
        CourseEntity liveCourse = buildCourse("user-1");
        liveCourse.setCourseType("LIVE");

        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(moduleRepository.findById("module-1")).thenReturn(buildModule());
        when(courseRepository.findById("course-1")).thenReturn(liveCourse);

        com.example.course_service.dto.response.ConferenceResponseDTO conferenceResponse =
                new com.example.course_service.dto.response.ConferenceResponseDTO();
        conferenceResponse.setConferenceId("session-1");
        when(liveClient.createConference(any(), anyString())).thenReturn(conferenceResponse);

        LessonEntity request = new LessonEntity();
        request.setModuleId("module-1");
        request.setTitle("Live Lesson");
        request.setLessonType("LIVE");

        java.util.Map<String, Object> result = lessonService.createLesson(request, TOKEN);

        assertTrue(result.get("message").toString().startsWith("Lesson Created Successfully"));
        ArgumentCaptor<LessonEntity> captor = ArgumentCaptor.forClass(LessonEntity.class);
        verify(lessonRepository).save(captor.capture());
        assertEquals("session-1", captor.getValue().getLiveSessionId());
    }

    @Test
    void createLesson_LiveType_InRecordedCourse_ThrowsException() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(moduleRepository.findById("module-1")).thenReturn(buildModule());
        when(courseRepository.findById("course-1")).thenReturn(buildCourse("user-1")); // RECORDED

        LessonEntity request = new LessonEntity();
        request.setModuleId("module-1");
        request.setTitle("Live Lesson");
        request.setLessonType("LIVE");

        assertThrows(IllegalArgumentException.class,
                () -> lessonService.createLesson(request, TOKEN));
    }

    // ================= MULTIPART =================

    @Test
    void initiateMultipartUpload_ReturnsResponse() {
        when(fileService.initiateMultipartUpload(anyString())).thenReturn("upload-123");

        com.example.course_service.dto.request.MultipartUploadInitRequest request =
                new com.example.course_service.dto.request.MultipartUploadInitRequest();

        var result = lessonService.initiateMultipartUpload(TOKEN, request);

        assertNotNull(result);
        assertEquals("upload-123", result.getUploadId());
    }

    @Test
    void generatePartUploadUrl_ReturnsUrl() {
        when(fileService.generatePartUploadUrl("key", "upload-123", 1)).thenReturn("https://presigned.url");

        String url = lessonService.generatePartUploadUrl(TOKEN, "upload-123", "key", 1);

        assertEquals("https://presigned.url", url);
    }

    @Test
    void abortMultipartUpload_CallsService() {
        com.example.course_service.dto.request.AbortMultipartUploadRequestDTO request =
                new com.example.course_service.dto.request.AbortMultipartUploadRequestDTO();
        request.setFileKey("key");
        request.setUploadId("upload-123");

        String result = lessonService.abortMultipartUpload(TOKEN, request);

        assertEquals("Upload aborted", result);
        verify(fileService).abortMultipartUpload("key", "upload-123");
    }
}
