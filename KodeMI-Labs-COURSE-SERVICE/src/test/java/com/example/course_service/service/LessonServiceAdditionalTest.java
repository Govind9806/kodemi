package com.example.course_service.service;

import com.example.course_service.dto.request.*;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.dto.response.MultipartUploadInitResponse;
import com.example.course_service.feign.LiveClient;
import com.example.course_service.model.LessonEntity;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LessonServiceAdditionalTest {

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

    // ================= MULTIPART UPLOAD =================

    @Test
    void initiateMultipartUpload_ReturnsResponse() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(fileService.initiateMultipartUpload(anyString())).thenReturn("upload-123");

        MultipartUploadInitRequest request = new MultipartUploadInitRequest();
        MultipartUploadInitResponse result = lessonService.initiateMultipartUpload(TOKEN, request);

        assertNotNull(result);
        assertEquals("upload-123", result.getUploadId());
    }

    @Test
    void generatePartUploadUrl_ReturnsUrl() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(fileService.generatePartUploadUrl("key", "upload-123", 1)).thenReturn("https://presigned.url");

        String url = lessonService.generatePartUploadUrl(TOKEN, "upload-123", "key", 1);

        assertEquals("https://presigned.url", url);
    }

    @Test
    void completeMultipartUpload_ReturnsResponse() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");

        CompleteMultipartUploadRequestDTO request = new CompleteMultipartUploadRequestDTO();
        request.setFileKey("key");
        request.setUploadId("upload-123");

        CompleteMultipartUploadRequestDTO.PartETag part = new CompleteMultipartUploadRequestDTO.PartETag();
        part.setPartNumber(1);
        part.setETag("etag-1");
        request.setParts(List.of(part));

        CompleteMultipartUploadResponse mockResponse =
                new CompleteMultipartUploadResponse("upload-123", "key", "done");
        when(fileService.completeMultipartUpload(eq("key"), eq("upload-123"), any())).thenReturn(mockResponse);

        CompleteMultipartUploadResponse result = lessonService.completeMultipartUpload(TOKEN, request);

        assertNotNull(result);
        assertEquals("key", result.getVideoKey());
    }

    @Test
    void abortMultipartUpload_CallsService() {
        AbortMultipartUploadRequestDTO request = new AbortMultipartUploadRequestDTO();
        request.setFileKey("key");
        request.setUploadId("upload-123");

        String result = lessonService.abortMultipartUpload(TOKEN, request);

        assertEquals("Upload aborted", result);
        verify(fileService).abortMultipartUpload("key", "upload-123");
    }

    @Test
    void saveUploadedContent_SavesContentItem() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("lesson-1");
        lesson.setContentKey(new ArrayList<>());

        when(lessonRepository.findById("lesson-1")).thenReturn(lesson);

        SaveUploadedContentRequest request = new SaveUploadedContentRequest();
        request.setLessonId("lesson-1");
        request.setFileKey("videos/lesson-1/video.mp4");
        request.setLabel("Main Video");

        String result = lessonService.saveUploadedContent(TOKEN, request);

        assertEquals("Content saved", result);
        verify(lessonRepository).save(lesson);
        assertEquals(1, lesson.getContentKey().size());
    }

    @Test
    void getDownloadUrl_ReturnsUrl() {
        when(fileService.generateDownloadUrl("videos/key.mp4")).thenReturn("https://cloudfront.com/signed");

        String url = lessonService.getDownloadUrl(TOKEN, "lesson-1", "videos/key.mp4");

        assertEquals("https://cloudfront.com/signed", url);
    }

    @Test
    void saveLiveRecording_SavesRecording() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("lesson-1");
        lesson.setContentKey(new ArrayList<>());

        when(lessonRepository.findById("lesson-1")).thenReturn(lesson);

        LiveRecordingRequest request = new LiveRecordingRequest();
        request.setLessonId("lesson-1");
        request.setRecordingKey("recordings/session-1.mp4");
        request.setDuration(3600);

        String result = lessonService.saveLiveRecording(request);

        assertEquals("Recording saved", result);
        verify(lessonRepository).save(lesson);
        assertEquals(3600, lesson.getDuration());
    }
}
