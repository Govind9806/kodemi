package com.example.course_service.service;

import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.service.impl.VideoProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoProcessingServiceAdditionalTest {

    private VideoProcessingService videoProcessingService;
    private S3Client s3Client;
    private FileService fileService;
    private LessonRepository lessonRepository;
    private CourseService courseService;

    @BeforeEach
    void setup() {
        s3Client = mock(S3Client.class);
        fileService = mock(FileService.class);
        lessonRepository = mock(LessonRepository.class);
        courseService = mock(CourseService.class);
        videoProcessingService = new VideoProcessingService(s3Client, fileService, lessonRepository, courseService, "ffprobe", "ffmpeg");
    }

    // ================= null guard combinations =================

    @Test
    void processUploadedVideoAsync_NullLesson_NullCourse_NullItem_ReturnsEarly() {
        videoProcessingService.processUploadedVideoAsync(null, null, null);
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void processUploadedVideoAsync_ValidLesson_NullCourse_ReturnsEarly() {
        videoProcessingService.processUploadedVideoAsync(new LessonEntity(), null, new LessonEntity.ContentItem());
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void processUploadedVideoAsync_NullLesson_ValidCourse_ReturnsEarly() {
        videoProcessingService.processUploadedVideoAsync(null, new CourseEntity(), new LessonEntity.ContentItem());
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void processUploadedVideoAsync_ValidLesson_ValidCourse_NullItem_ReturnsEarly() {
        videoProcessingService.processUploadedVideoAsync(new LessonEntity(), new CourseEntity(), null);
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void processUploadedVideoAsync_ItemWithNullKey_ReturnsEarly() {
        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setKey(null);
        videoProcessingService.processUploadedVideoAsync(new LessonEntity(), new CourseEntity(), item);
        verifyNoInteractions(lessonRepository);
    }

    // ================= S3 download failure → markFailed =================

    @Test
    void processUploadedVideoAsync_S3Fails_SetsStatusFailed_AndSaves() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L1");
        lesson.setContentKey(new ArrayList<>());

        CourseEntity course = new CourseEntity();
        course.setCourseId("C1");

        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setKey("videos/test.mp4");

        when(fileService.getBucketName()).thenReturn("test-bucket");
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenThrow(new RuntimeException("S3 error"));

        videoProcessingService.processUploadedVideoAsync(lesson, course, item);

        verify(lessonRepository).save(lesson);
        assertEquals("FAILED", item.getStatus());
    }

    @Test
    void processUploadedVideoAsync_S3Fails_LessonWithNullContentKey_StillSaves() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L2");
        lesson.setContentKey(null);

        CourseEntity course = new CourseEntity();
        course.setCourseId("C1");

        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setKey("videos/test.mp4");

        when(fileService.getBucketName()).thenReturn("test-bucket");
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenThrow(new RuntimeException("S3 error"));

        videoProcessingService.processUploadedVideoAsync(lesson, course, item);

        verify(lessonRepository).save(lesson);
        assertEquals("FAILED", item.getStatus());
    }

    // ================= markFailed null guards =================

    @Test
    void processUploadedVideoAsync_S3Fails_NullLessonId_DoesNotThrow() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(null);
        lesson.setContentKey(new ArrayList<>());

        CourseEntity course = new CourseEntity();
        course.setCourseId("C1");

        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setKey("videos/test.mp4");

        when(fileService.getBucketName()).thenReturn("test-bucket");
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertDoesNotThrow(() ->
                videoProcessingService.processUploadedVideoAsync(lesson, course, item));
    }

    // ================= multiple S3 failures verify only one save =================

    @Test
    void processUploadedVideoAsync_S3Fails_SaveCalledExactlyOnce() {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId("L3");
        lesson.setContentKey(new ArrayList<>());

        CourseEntity course = new CourseEntity();
        course.setCourseId("C1");

        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setKey("videos/test.mp4");

        when(fileService.getBucketName()).thenReturn("test-bucket");
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenThrow(new RuntimeException("S3 error"));

        videoProcessingService.processUploadedVideoAsync(lesson, course, item);

        verify(lessonRepository, times(1)).save(lesson);
        verifyNoInteractions(courseService);
    }
}
