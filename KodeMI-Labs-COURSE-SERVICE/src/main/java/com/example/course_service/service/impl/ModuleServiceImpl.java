package com.example.course_service.service.impl;

import com.example.course_service.dto.notification.NotificationChannel;
import com.example.course_service.dto.notification.NotificationRequest;
import com.example.course_service.dto.notification.NotificationType;
import com.example.course_service.dto.response.LessonResponseDTO;
import com.example.course_service.dto.response.ModuleResponseDTO;
import com.example.course_service.exception.CourseNotFoundException;
import com.example.course_service.exception.ForbiddenException;
import com.example.course_service.exception.ModuleNotFoundException;
import com.example.course_service.exception.NullException;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.model.ModuleEntity;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.ModuleRepository;
import com.example.course_service.service.FileService;
import com.example.course_service.service.ModuleService;
import com.example.course_service.service.notification.NotificationPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ModuleServiceImpl implements ModuleService {

    private static final String NULL_BODY_MESSAGE = "Null Body.";
    private static final String REFERENCE_TYPE_COURSE = "COURSE";
    private static final String NOTIFICATION_TITLE_CONTENT_UPDATED = "Course Content Updated";

    private final CacheManager cacheManager;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final FileService fileService;
    private final EnrollmentClient enrollmentClient;
    private final NotificationPublisher notificationPublisher;

    @Autowired
    public ModuleServiceImpl(ModuleRepository moduleRepository,
                             LessonRepository lessonRepository, CourseRepository courseRepository,
                             FileService fileService,
                             EnrollmentClient enrollmentClient,
                             NotificationPublisher notificationPublisher,
                             @Autowired(required = false) CacheManager cacheManager) {
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
        this.fileService = fileService;
        this.enrollmentClient = enrollmentClient;
        this.notificationPublisher = notificationPublisher;
        this.cacheManager = cacheManager;
    }

    /**
     * Convenience overload for callers (e.g. tests) that construct this service
     * directly without a CacheManager bean available.
     */
    public ModuleServiceImpl(ModuleRepository moduleRepository,
                             LessonRepository lessonRepository, CourseRepository courseRepository,
                             FileService fileService,
                             EnrollmentClient enrollmentClient,
                             NotificationPublisher notificationPublisher) {
        this(moduleRepository, lessonRepository, courseRepository, fileService, enrollmentClient, notificationPublisher, null);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "modulesByCourse", key = "#request.courseId"),
            @CacheEvict(value = "courseDetails", key = "#request.courseId"),
            @CacheEvict(value = "liveCourseDetails", key = "#request.courseId"),
            @CacheEvict(value = "allCourses", allEntries = true),
            @CacheEvict(value = "verifiedCourses", allEntries = true)
    })
    public Map<String, Object> createModule(ModuleEntity request, String userId) {
        log.info("Entering createModule for courseId: {}, userId: {}", request != null ? request.getCourseId() : null, userId);

        if (request == null) {
            throw new NullException(NULL_BODY_MESSAGE);
        }
        CourseEntity course = courseRepository.findById(request.getCourseId());

        if (course == null) {
            log.warn("Course not found with id: {}", request.getCourseId());
            throw new CourseNotFoundException(request.getCourseId());
        }
        if (!course.getCreatorId().equals(userId)) {
            log.warn("User {} is not allowed to add module to course {}", userId, course.getCourseId());
            throw new ForbiddenException("You are not allowed to add module to this course");
        }

        ModuleEntity module = new ModuleEntity();
        module.setModuleId(UUID.randomUUID().toString());
        module.setCourseId(request.getCourseId());
        module.setUserId(userId);
        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());
        module.setOrderIndex(request.getOrderIndex());
        module.setCreatedAt(new Date());
        module.setUpdatedAt(new Date());

        moduleRepository.save(module);
        log.info("Module created successfully. moduleId: {}, courseId: {}", module.getModuleId(), module.getCourseId());

        notifyEnrolledLearners(course.getCourseId(),
                "New module \"" + module.getTitle() + "\" added to course \"" + course.getTitle() + "\".",
                "new module");

        return Map.of(
                "message", "Module Created Successfully",
                "moduleId", module.getModuleId(),
                "courseId", module.getCourseId());
    }

    @Override
    public ModuleResponseDTO getModuleById(String moduleId) {
        log.info("Entering getModuleById for moduleId: {}", moduleId);
        ModuleEntity module = getModuleOrThrow(moduleId);
        List<LessonEntity> lessons = lessonRepository.findByModuleId(moduleId);
        ModuleResponseDTO result = toDTO(module, lessons);
        log.info("Exiting getModuleById for moduleId: {} successfully", moduleId);
        return result;
    }

    @Override
    public List<ModuleResponseDTO> getAllModules() {
        log.info("Entering getAllModules");
        List<ModuleResponseDTO> result = new ArrayList<>();
        for (ModuleEntity m : moduleRepository.findAll()) {
            result.add(toDTO(m, lessonRepository.findByModuleId(m.getModuleId())));
        }
        log.info("Exiting getAllModules. Found {} modules.", result.size());
        return result;
    }

    @Override
    @Cacheable(value = "modulesByCourse", key = "#courseId")
    public List<ModuleResponseDTO> getModulesByCourse(String courseId) {
        log.info("Entering getModulesByCourse for courseId: {}", courseId);
        List<ModuleEntity> modules = new ArrayList<>(moduleRepository.findByCourseId(courseId));
        modules.sort(Comparator.comparingInt(m -> m.getOrderIndex() == null ? 0 : m.getOrderIndex()));

        List<ModuleResponseDTO> result = new ArrayList<>();
        for (ModuleEntity m : modules) {
            result.add(toDTO(m, lessonRepository.findByModuleId(m.getModuleId())));
        }
        log.info("Exiting getModulesByCourse for courseId: {}. Found {} modules.", courseId, result.size());
        return result;
    }

    @Override
    public String updateModule(String userId, String moduleId, ModuleEntity request) {
        log.info("Entering updateModule for moduleId: {}, userId: {}", moduleId, userId);
        if (request == null) {
            throw new NullException(NULL_BODY_MESSAGE);
        }
        ModuleEntity existing = getModuleOrThrow(moduleId);
        if (!userId.equals(existing.getUserId())) {
            log.warn("User {} forbidden from updating module {}", userId, moduleId);
            throw new ForbiddenException("Forbidden.");
        }
        if (request.getTitle() != null) {
            existing.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getOrderIndex() != null) {
            existing.setOrderIndex(request.getOrderIndex());
        }
        existing.setUpdatedAt(new Date());
        moduleRepository.save(existing);
        log.info("Module {} updated successfully.", moduleId);

        CourseEntity course = courseRepository.findById(existing.getCourseId());
        if (course != null) {
            notifyEnrolledLearners(course.getCourseId(),
                    "Module \"" + existing.getTitle() + "\" has been updated in course \"" + course.getTitle() + "\".",
                    "updated module");
        }

        evictCourseCaches(existing.getCourseId());
        return "Module Updated Successfully.";
    }

    /**
     * Looks up enrolled learners for the course and publishes a "content updated"
     * notification to them, swallowing and logging any failure so a notification
     * outage never breaks the calling business operation.
     */
    private void notifyEnrolledLearners(String courseId, String message, String logContext) {
        try {
            List<String> learners = enrollmentClient.getEnrolledLearners(courseId);
            NotificationRequest notifReq = NotificationRequest.builder()
                    .title(NOTIFICATION_TITLE_CONTENT_UPDATED)
                    .message(message)
                    .type(NotificationType.COURSE_CONTENT_UPDATED)
                    .channels(List.of(NotificationChannel.IN_APP))
                    .referenceId(courseId)
                    .referenceType(REFERENCE_TYPE_COURSE)
                    .build();
            notificationPublisher.publishToUsers(learners, notifReq);
        } catch (Exception e) {
            log.error("Failed to notify learners for {}", logContext, e);
        }
    }

    private void evictCourseCaches(String courseId) {
        if (cacheManager == null || courseId == null) {
            return;
        }
        evictFromCache("modulesByCourse", courseId);
        evictFromCache("courseDetails", courseId);
        evictFromCache("liveCourseDetails", courseId);
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

    private ModuleEntity getModuleOrThrow(String moduleId) {
        ModuleEntity module = moduleRepository.findById(moduleId);
        if (module == null) {
            log.warn("ModuleNotFoundException for moduleId: {}", moduleId);
            throw new ModuleNotFoundException(moduleId);
        }
        return module;
    }

    private ModuleResponseDTO toDTO(ModuleEntity m, List<LessonEntity> lessons) {
        List<LessonEntity> sortedLessons = new ArrayList<>(lessons);
        sortedLessons.sort(Comparator.comparingInt(l -> l.getOrderIndex() == null ? 0 : l.getOrderIndex()));

        List<LessonResponseDTO> lessonDTOs = new ArrayList<>();
        for (LessonEntity l : sortedLessons) {
            lessonDTOs.add(LessonResponseDTO.builder()
                    .lessonId(l.getLessonId())
                    .moduleId(l.getModuleId())
                    .title(l.getTitle())
                    .description(l.getDescription())
                    .duration(l.getDuration())
                    .orderIndex(l.getOrderIndex())
                    .videoKey(l.getVideoKey())
                    .videoUrl(l.getVideoKey() == null ? null : fileService.generateDownloadUrl(l.getVideoKey()))
                    .contentKey(l.getContentKey())
                    .lessonType(l.getLessonType())
                    .liveSessionId(l.getLiveSessionId())
                    .scheduledAt(l.getScheduledAt())
                    .createdAt(l.getCreatedAt())
                    .updatedAt(l.getUpdatedAt())
                    .build());
        }

        return ModuleResponseDTO.builder()
                .moduleId(m.getModuleId())
                .courseId(m.getCourseId())
                .title(m.getTitle())
                .description(m.getDescription())
                .orderIndex(m.getOrderIndex())
                .lessonCount(lessonDTOs.size())
                .lessons(lessonDTOs)
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}