package com.example.course_service.service.impl;

import com.example.course_service.dto.request.AbortMultipartUploadRequestDTO;
import com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO;
import com.example.course_service.dto.request.CreateConferenceRequest;
import com.example.course_service.dto.request.LiveRecordingRequest;
import com.example.course_service.dto.request.MultipartUploadInitRequest;
import com.example.course_service.dto.request.MultipartUploadPartETag;
import com.example.course_service.dto.request.SaveUploadedContentRequest;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.dto.response.ConferenceResponseDTO;
import com.example.course_service.dto.response.LessonResponseDTO;
import com.example.course_service.dto.response.MultipartUploadInitResponse;
import com.example.course_service.exception.ForbiddenException;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.feign.LiveClient;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.model.ModuleEntity;
import com.example.course_service.dto.notification.NotificationChannel;
import com.example.course_service.dto.notification.NotificationRequest;
import com.example.course_service.dto.notification.NotificationType;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.ModuleRepository;
import com.example.course_service.service.CourseService;
import com.example.course_service.service.FileService;
import com.example.course_service.service.LessonService;
import com.example.course_service.service.notification.NotificationPublisher;
import com.example.course_service.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class LessonServiceImpl implements LessonService {

    private static final String LESSON_TYPE_LIVE = "LIVE";
    private static final String COURSE_TYPE_LIVE = "LIVE";
    private static final String SOURCE_TYPE_LIVE_COURSE = "LIVE_COURSE";
    private static final String CONTENT_TYPE_VIDEO = "VIDEO";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_READY = "READY";
    private static final String REFERENCE_TYPE_COURSE = "COURSE";
    private static final int DEFAULT_MAX_CONFERENCE_PARTICIPANTS = 15;

    private final CacheManager cacheManager;
    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final FileService fileService;
    private final JwtUtil jwtUtil;
    private final LiveClient liveClient;
    private final EnrollmentClient enrollmentClient;
    private final NotificationPublisher notificationPublisher;
    private final CourseService courseService;
    private final VideoProcessingService videoProcessingService;

    @Autowired
    public LessonServiceImpl(LessonRepository lessonRepository,
                             ModuleRepository moduleRepository,
                             CourseRepository courseRepository,
                             FileService fileService,
                             JwtUtil jwtUtil,
                             LiveClient liveClient,
                             EnrollmentClient enrollmentClient,
                             NotificationPublisher notificationPublisher,
                             @Lazy CourseService courseService,
                             @Lazy VideoProcessingService videoProcessingService,
                             @Autowired(required = false) CacheManager cacheManager) {
        this.lessonRepository = lessonRepository;
        this.moduleRepository = moduleRepository;
        this.courseRepository = courseRepository;
        this.fileService = fileService;
        this.jwtUtil = jwtUtil;
        this.liveClient = liveClient;
        this.enrollmentClient = enrollmentClient;
        this.notificationPublisher = notificationPublisher;
        this.courseService = courseService;
        this.videoProcessingService = videoProcessingService;
        this.cacheManager = cacheManager;
    }

    /**
     * Convenience overload for callers (e.g. tests) that construct this service
     * directly without a CacheManager bean available.
     */
    public LessonServiceImpl(LessonRepository lessonRepository,
                             ModuleRepository moduleRepository,
                             CourseRepository courseRepository,
                             FileService fileService,
                             JwtUtil jwtUtil,
                             LiveClient liveClient,
                             EnrollmentClient enrollmentClient,
                             NotificationPublisher notificationPublisher,
                             @Lazy CourseService courseService,
                             @Lazy VideoProcessingService videoProcessingService) {
        this(lessonRepository, moduleRepository, courseRepository, fileService, jwtUtil, liveClient, enrollmentClient, notificationPublisher, courseService, videoProcessingService, null);
    }

    @Override
    @Transactional
    public Map<String, Object> createLesson(Object req, String token) {
        LessonEntity request = (LessonEntity) req;
        String userId = jwtUtil.extractUserId(token);

        ModuleEntity module = moduleRepository.findById(request.getModuleId());
        CourseEntity course = courseRepository.findById(module.getCourseId());

        if (!course.getCreatorId().equals(userId)) {
            throw new ForbiddenException("Forbidden");
        }

        LessonEntity lesson = buildNewLesson(request);

        if (LESSON_TYPE_LIVE.equals(request.getLessonType())) {
            attachLiveSession(request, course, module, lesson, token);
        }

        lessonRepository.save(lesson);
        log.info("[LESSON CREATED] lessonId: {}, videoKey: {}", lesson.getLessonId(), lesson.getVideoKey());

        CompletableFuture.runAsync(() -> notifyEnrolledLearners(
                course.getCourseId(),
                "New Lesson Added",
                "New lesson '" + lesson.getTitle() + "' added in " + course.getTitle() + ".",
                NotificationType.NEW_LESSON_ADDED));

        courseService.refreshCourseStats(course.getCourseId());
        evictLessonCaches(lesson.getModuleId(), course.getCourseId(), null);

        return Map.of(
                "message", "Lesson Created Successfully",
                "lessonId", lesson.getLessonId(),
                "moduleId", lesson.getModuleId(),
                "courseId", course.getCourseId());
    }

    private LessonEntity buildNewLesson(LessonEntity request) {
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(UUID.randomUUID().toString());
        lesson.setModuleId(request.getModuleId());
        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setLessonType(request.getLessonType());
        lesson.setScheduledAt(request.getScheduledAt());
        lesson.setContentKey(new ArrayList<>());
        lesson.setDuration(request.getDuration() != null ? request.getDuration() : 0);
        lesson.setVideoKey(request.getVideoKey());
        lesson.setCreatedAt(new Date());
        lesson.setUpdatedAt(new Date());
        return lesson;
    }

    private void attachLiveSession(LessonEntity request, CourseEntity course, ModuleEntity module, LessonEntity lesson, String token) {
        if (!COURSE_TYPE_LIVE.equals(course.getCourseType())) {
            throw new IllegalArgumentException("LIVE lessons only allowed in LIVE courses");
        }

        if (request.getLiveSessionId() != null && !request.getLiveSessionId().isBlank()) {
            lesson.setLiveSessionId(request.getLiveSessionId());
            return;
        }

        CreateConferenceRequest confReq = new CreateConferenceRequest();
        confReq.setTitle(request.getTitle());
        confReq.setDescription(request.getDescription() != null ? request.getDescription() : request.getTitle());
        confReq.setScheduledAt(request.getScheduledAt() != null ? request.getScheduledAt().toString() : new Date().toString());
        confReq.setMaxParticipants(DEFAULT_MAX_CONFERENCE_PARTICIPANTS);
        confReq.setCourseId(course.getCourseId());
        confReq.setModuleId(module.getModuleId());
        confReq.setLessonId(lesson.getLessonId());
        confReq.setSourceType(SOURCE_TYPE_LIVE_COURSE);

        ConferenceResponseDTO res = liveClient.createConference(confReq, token);
        lesson.setLiveSessionId(res.getConferenceId());
        log.info("Created LIVE course conference for lessonId={} mapped to conferenceId={}", lesson.getLessonId(), res.getConferenceId());
    }

    /**
     * Looks up enrolled learners for the course and publishes a notification to them,
     * swallowing and logging any failure so a notification outage never breaks the
     * calling business operation.
     */
    private void notifyEnrolledLearners(String courseId, String title, String message, NotificationType type) {
        try {
            List<String> learnerIds = enrollmentClient.getEnrolledLearners(courseId);
            if (learnerIds == null || learnerIds.isEmpty()) {
                return;
            }
            NotificationRequest notifReq = NotificationRequest.builder()
                    .title(title)
                    .message(message)
                    .type(type)
                    .channels(List.of(NotificationChannel.IN_APP))
                    .referenceId(courseId)
                    .referenceType(REFERENCE_TYPE_COURSE)
                    .build();
            notificationPublisher.publishToUsers(learnerIds, notifReq);
        } catch (Exception e) {
            log.error("Failed to notify enrolled learners for course {}", courseId, e);
        }
    }

    @Override
    @Transactional
    public String updateLesson(String token, String lessonId, Object req) {
        LessonEntity request = (LessonEntity) req;
        LessonEntity lesson = lessonRepository.findById(lessonId);

        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setUpdatedAt(new Date());

        lessonRepository.save(lesson);

        ModuleEntity module = moduleRepository.findById(lesson.getModuleId());
        CourseEntity course = null;
        if (module != null && module.getCourseId() != null) {
            course = courseRepository.findById(module.getCourseId());
            courseService.refreshCourseStats(module.getCourseId());
        }

        if (course != null) {
            notifyEnrolledLearners(
                    course.getCourseId(),
                    "Course Content Updated",
                    "Lesson \"" + lesson.getTitle() + "\" has been updated in course \"" + course.getTitle() + "\".",
                    NotificationType.COURSE_CONTENT_UPDATED);
        }

        evictLessonCaches(lesson.getModuleId(), module != null ? module.getCourseId() : null, lessonId);
        return "Lesson updated";
    }

    @Override
    public Map<String, String> generateUploadUrl(String token, String lessonId, String fileName) {
        String key = "lesson/" + lessonId + "/" + UUID.randomUUID() + "-" + fileName;
        String url = fileService.generateUploadUrl(key, "video/mp4");
        return Map.of(
                "uploadUrl", url,
                "fileKey", key);
    }

    @Override
    public MultipartUploadInitResponse initiateMultipartUpload(String token, MultipartUploadInitRequest request) {
        String fileKey = "courses/videos/" + UUID.randomUUID() + ".mp4";
        String uploadId = fileService.initiateMultipartUpload(fileKey);

        log.info("[MULTIPART INIT] uploadId: {}, fileKey: {}", uploadId, fileKey);
        return new MultipartUploadInitResponse(uploadId, fileKey, "Upload initiated");
    }

    @Override
    public String generatePartUploadUrl(String token, String uploadId, String fileKey, int partNumber) {
        String url = fileService.generatePartUploadUrl(fileKey, uploadId, partNumber);
        log.info("[PART PRESIGNED URL] Generated for uploadId: {}, partNumber: {}", uploadId, partNumber);
        return url;
    }

    @Override
    public CompleteMultipartUploadResponse completeMultipartUpload(String token, CompleteMultipartUploadRequestDTO request) {
        List<MultipartUploadPartETag> parts = new ArrayList<>();
        for (CompleteMultipartUploadRequestDTO.PartETag p : request.getParts()) {
            parts.add(new MultipartUploadPartETag(p.getPartNumber(), p.getETag()));
        }

        CompleteMultipartUploadResponse response = fileService.completeMultipartUpload(request.getFileKey(), request.getUploadId(), parts);
        log.info("[MULTIPART COMPLETE] videoKey: {}", response.getVideoKey());
        return response;
    }

    @Override
    public String abortMultipartUpload(String token, AbortMultipartUploadRequestDTO request) {
        fileService.abortMultipartUpload(request.getFileKey(), request.getUploadId());
        return "Upload aborted";
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "lessonsByModule", allEntries = true),
            @CacheEvict(value = "lessonDetails", allEntries = true),
            @CacheEvict(value = "courseDetails", allEntries = true),
            @CacheEvict(value = "liveCourseDetails", allEntries = true),
            @CacheEvict(value = "allCourses", allEntries = true),
            @CacheEvict(value = "verifiedCourses", allEntries = true)
    })
    public String saveUploadedContent(String token, SaveUploadedContentRequest request) {
        LessonEntity lesson = lessonRepository.findById(request.getLessonId());
        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setType(CONTENT_TYPE_VIDEO);
        item.setKey(request.getFileKey());
        item.setStatus(STATUS_PROCESSING);
        item.setLabel(request.getLabel());

        if (lesson.getContentKey() == null) {
            lesson.setContentKey(new ArrayList<>());
        }
        lesson.getContentKey().add(item);
        lessonRepository.save(lesson);

        ModuleEntity module = moduleRepository.findById(lesson.getModuleId());
        CourseEntity course = null;
        if (module != null && module.getCourseId() != null) {
            course = courseRepository.findById(module.getCourseId());
        }
        videoProcessingService.processUploadedVideoAsync(lesson, course, item);

        return "Content saved";
    }

    @Override
    @Cacheable(value = "lessonDetails", key = "#lessonId")
    public LessonResponseDTO getLessonById(String lessonId, String token) {
        LessonEntity lesson = lessonRepository.findById(lessonId);
        ModuleEntity module = moduleRepository.findById(lesson.getModuleId());
        String courseId = (module != null) ? module.getCourseId() : null;
        return toLessonResponseDTO(lesson, courseId);
    }

    @Override
    public String getDownloadUrl(String token, String lessonId, String fileKey) {
        return fileService.generateDownloadUrl(fileKey);
    }

    @Override
    @Cacheable(value = "lessonsByModule", key = "#moduleId")
    public List<LessonResponseDTO> getLessonsByModule(String moduleId, String token) {
        List<LessonEntity> lessons = lessonRepository.findByModuleId(moduleId);
        ModuleEntity module = moduleRepository.findById(moduleId);
        String courseId = (module != null) ? module.getCourseId() : null;
        List<LessonResponseDTO> result = new ArrayList<>();
        for (LessonEntity l : lessons) {
            result.add(toLessonResponseDTO(l, courseId));
        }
        return result;
    }

    private LessonResponseDTO toLessonResponseDTO(LessonEntity l, String courseId) {
        return LessonResponseDTO.builder()
                .lessonId(l.getLessonId())
                .moduleId(l.getModuleId())
                .courseId(courseId)
                .title(l.getTitle())
                .description(l.getDescription())
                .duration(l.getDuration())
                .orderIndex(l.getOrderIndex())
                .videoKey(l.getVideoKey())
                .videoUrl(l.getVideoKey() == null ? null : fileService.generateDownloadUrl(l.getVideoKey()))
                .lessonType(l.getLessonType())
                .liveSessionId(l.getLiveSessionId())
                .scheduledAt(l.getScheduledAt())
                .contentKey(l.getContentKey())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public String saveLiveRecording(LiveRecordingRequest request) {
        LessonEntity lesson = lessonRepository.findById(request.getLessonId());
        LessonEntity.ContentItem item = new LessonEntity.ContentItem();
        item.setType(CONTENT_TYPE_VIDEO);
        item.setKey(request.getRecordingKey());
        item.setStatus(STATUS_READY);
        item.setLabel("Live Recording");

        if (lesson.getContentKey() == null) {
            lesson.setContentKey(new ArrayList<>());
        }
        lesson.getContentKey().add(item);
        lesson.setDuration(request.getDuration());
        lessonRepository.save(lesson);

        ModuleEntity module = moduleRepository.findById(lesson.getModuleId());
        if (module != null && module.getCourseId() != null) {
            courseService.refreshCourseStats(module.getCourseId());
        }

        evictLessonCaches(lesson.getModuleId(), module != null ? module.getCourseId() : null, request.getLessonId());
        return "Recording saved";
    }

    private void evictLessonCaches(String moduleId, String courseId, String lessonId) {
        if (cacheManager == null) {
            return;
        }
        if (moduleId != null) {
            evictFromCache("lessonsByModule", moduleId);
        }
        if (lessonId != null) {
            evictFromCache("lessonDetails", lessonId);
        }
        if (courseId != null) {
            evictFromCache("courseDetails", courseId);
            evictFromCache("liveCourseDetails", courseId);
        }
        clearCache("allCourses");
        clearCache("verifiedCourses");
    }

    private void evictFromCache(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}