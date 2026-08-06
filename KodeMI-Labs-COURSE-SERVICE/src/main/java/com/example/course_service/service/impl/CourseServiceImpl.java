package com.example.course_service.service.impl;

import com.example.course_service.dto.notification.NotificationChannel;
import com.example.course_service.dto.notification.NotificationRequest;
import com.example.course_service.dto.notification.NotificationType;
import com.example.course_service.dto.request.AbortMultipartUploadRequestDTO;
import com.example.course_service.dto.request.CompleteMultipartUploadRequestDTO;
import com.example.course_service.dto.request.CourseModerationRequest;
import com.example.course_service.dto.request.MultipartUploadInitRequest;
import com.example.course_service.dto.request.MultipartUploadPartETag;
import com.example.course_service.dto.request.UploadAbortRequest;
import com.example.course_service.dto.request.UploadCompleteRequest;
import com.example.course_service.dto.request.UploadInitRequest;
import com.example.course_service.dto.request.UploadPresignedUrlRequest;
import com.example.course_service.dto.response.CompleteMultipartUploadResponse;
import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.LessonResponseDTO;
import com.example.course_service.dto.response.ModuleResponseDTO;
import com.example.course_service.dto.response.MultipartUploadInitResponse;
import com.example.course_service.dto.response.ReviewResponseDTO;
import com.example.course_service.dto.response.TrainerResponseDTO;
import com.example.course_service.dto.response.UploadInitResponse;
import com.example.course_service.dto.response.UserEnrollmentResponse;
import com.example.course_service.exception.CourseNotFoundException;
import com.example.course_service.exception.ForbiddenException;
import com.example.course_service.exception.NullException;
import com.example.course_service.exception.ThumbnailNotFoundException;
import com.example.course_service.exception.TokenNotFoundException;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.feign.TrainerClient;
import com.example.course_service.model.CategoryEntity;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.FileType;
import com.example.course_service.model.LessonEntity;
import com.example.course_service.model.ModuleEntity;
import com.example.course_service.model.ReviewEntity;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LessonRepository;
import com.example.course_service.repository.ModuleRepository;
import com.example.course_service.repository.ReviewRepository;
import com.example.course_service.service.CourseService;
import com.example.course_service.service.FileService;
import com.example.course_service.service.UploadService;
import com.example.course_service.service.notification.NotificationPublisher;
import com.example.course_service.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private static final String NULL_BODY_MESSAGE = "Null Body.";
    private static final String ZERO_MINUTES = "0 Minutes";

    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final ReviewRepository reviewRepository;
    private final TrainerClient trainerClient;
    private final EnrollmentClient enrollmentClient;
    private final FileService fileService;
    private final UploadService uploadService;
    private final NotificationPublisher notificationPublisher;
    private final JwtUtil jwtUtil;

    private final ObjectProvider<CourseServiceImpl> selfProvider;

    public CourseServiceImpl(CourseRepository courseRepository,
                             CategoryRepository categoryRepository,
                             ModuleRepository moduleRepository,
                             LessonRepository lessonRepository,
                             ReviewRepository reviewRepository,
                             TrainerClient trainerClient,
                             EnrollmentClient enrollmentClient,
                             FileService fileService,
                             UploadService uploadService,
                             NotificationPublisher notificationPublisher,
                             JwtUtil jwtUtil,
                             ObjectProvider<CourseServiceImpl> selfProvider) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.reviewRepository = reviewRepository;
        this.trainerClient = trainerClient;
        this.enrollmentClient = enrollmentClient;
        this.fileService = fileService;
        this.uploadService = uploadService;
        this.notificationPublisher = notificationPublisher;
        this.jwtUtil = jwtUtil;
        this.selfProvider = selfProvider;
    }

    private CourseServiceImpl getSelf() {
        return selfProvider.getIfAvailable(() -> this);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allCourses", allEntries = true),
            @CacheEvict(value = "instructorCourses", allEntries = true),
            @CacheEvict(value = "verifiedCourses", allEntries = true),
            @CacheEvict(value = "coursesByCategory", allEntries = true)
    })
    public Map<String, Object> createCourse(String token, CourseEntity request, MultipartFile demoVideo, MultipartFile thumbnail) {
        return processCourseCreation(token, request, demoVideo, thumbnail, false);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allCourses", allEntries = true),
            @CacheEvict(value = "instructorCourses", allEntries = true),
            @CacheEvict(value = "verifiedCourses", allEntries = true),
            @CacheEvict(value = "coursesByCategory", allEntries = true)
    })
    public Map<String, Object> createLiveCourse(String token, CourseEntity request, MultipartFile demoVideo, MultipartFile thumbnail) {
        return processCourseCreation(token, request, demoVideo, thumbnail, true);
    }

    private Map<String, Object> processCourseCreation(String token, CourseEntity request, MultipartFile demoVideo, MultipartFile thumbnail, boolean isLive) {
        String logPrefix = isLive ? "Live Course" : "Course";
        log.info("Starting {} Creation Process", logPrefix);
        Instant start = Instant.now();

        if (request == null) {
            throw new NullException(NULL_BODY_MESSAGE);
        }
        if (token == null) {
            throw new TokenNotFoundException("Token is required.");
        }

        Instant feignStart = Instant.now();
        TrainerResponseDTO trainer = trainerClient.getTrainer(token);
        String ownerId = trainer.getUserId();
        log.info("Feign Call took {}ms", Duration.between(feignStart, Instant.now()).toMillis());

        String courseId = UUID.randomUUID().toString();

        String thumbnailKey = resolveOrUploadAsset(
                request.getThumbnailKey(), thumbnail, token, ownerId, courseId,
                FileType.THUMBNAIL, () -> new ThumbnailNotFoundException("Course thumbnail is required."));

        String demoVideoKey = resolveOrUploadAsset(
                request.getDemoVideoKey(), demoVideo, token, ownerId, courseId,
                FileType.DEMO_VIDEO, () -> new IllegalArgumentException("Demo video is required (either as a file or a pre-uploaded key)"));

        Instant dbStart = Instant.now();
        CourseEntity course = new CourseEntity();
        course.setCourseId(courseId);
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCategoryId(request.getCategoryId());
        course.setCategoryName(fetchCategoryName(request.getCategoryId()));

        course.setCreatorId(trainer.getUserId());
        course.setCreatorName(trainer.getFullName());
        course.setLanguage(request.getLanguage());
        course.setSkillLevel(request.getSkillLevel());
        course.setPrice(request.getPrice());
        course.setThumbnailKey(thumbnailKey);
        course.setDemoVideoKey(demoVideoKey);
        course.setCategory(request.getCategory());
        course.setSubCategory(request.getSubCategory());
        course.setTopic(request.getTopic());
        course.setWelcomeMessage(request.getWelcomeMessage());
        course.setStatus("UNVERIFIED");
        course.setIsVerified(false);
        course.setLessonCount(0);
        course.setTotalReviews(0);
        course.setDurationLabel(ZERO_MINUTES);
        course.setAverageRating(0.0);
        course.setCourseType(isLive ? "LIVE" : "RECORDED");
        course.setCreatedAt(Instant.now());
        course.setUpdatedAt(Instant.now());

        courseRepository.save(course);
        notifyAdminEmitters();
        log.info("DB Save took {}ms", Duration.between(dbStart, Instant.now()).toMillis());

        String notificationTitle = isLive ? "New Live Course Submitted" : "New Course Submitted";
        String notificationMessage = (isLive ? "New live course submitted for approval: " : "New course submitted for approval: ") + course.getTitle();
        publishNotification(trainer.getUserId(), notificationTitle, notificationMessage,
                NotificationType.COURSE_SUBMITTED, "COURSE", course.getCourseId());

        log.info("Total {} Creation took {}ms", logPrefix, Duration.between(start, Instant.now()).toMillis());

        return Map.of(
                "message", logPrefix + " Created Successfully",
                "courseId", course.getCourseId());
    }

    private String resolveOrUploadAsset(String existingKey, MultipartFile file, String token, String ownerId,
                                        String courseId, FileType fileType,
                                        java.util.function.Supplier<? extends RuntimeException> missingAssetException) {
        if (file != null && !file.isEmpty()) {
            return uploadService.uploadFileDirectly(token, file, fileType, courseId);
        }
        if (existingKey != null && !existingKey.isEmpty()) {
            uploadService.validateAndConsumeUpload(existingKey, ownerId, courseId);
            return existingKey;
        }
        throw missingAssetException.get();
    }

    private String fetchCategoryName(String categoryId) {
        try {
            CategoryEntity categoryEntity = categoryRepository.findById(categoryId);
            return categoryEntity != null ? categoryEntity.getName() : null;
        } catch (Exception e) {
            log.warn("Could not fetch category name for categoryId {}: {}", categoryId, e.getMessage());
            return null;
        }
    }

    @Override
    @Cacheable(value = "allCourses", key = "'all'")
    public List<CourseResponseDTO> getAllCourses() {
        List<CourseResponseDTO> result = new ArrayList<>();
        for (CourseEntity course : courseRepository.findAll()) {
            if (Boolean.TRUE.equals(course.getIsVerified())) {
                result.add(toDTO(course));
            }
        }
        return result;
    }

    @Override
    @Cacheable(value = "instructorCourses", key = "#creatorId")
    public List<CourseResponseDTO> getCoursesByCreatorId(String creatorId) {
        return getSelf().getCoursesByCreatorId(creatorId, null);
    }

    @Override
    @Cacheable(value = "instructorCourses", key = "#creatorId + (#courseType != null ? '-' + #courseType : '')")
    public List<CourseResponseDTO> getCoursesByCreatorId(String creatorId, String courseType) {
        List<CourseResponseDTO> result = new ArrayList<>();
        for (CourseEntity c : courseRepository.findAll()) {
            if (creatorId.equals(c.getCreatorId())
                    && (courseType == null || courseType.trim().isEmpty() || courseType.equalsIgnoreCase(c.getCourseType()))) {
                CourseResponseDTO dto = toDTO(c);
                if ("LIVE".equalsIgnoreCase(c.getCourseType())) {
                    dto.setModules(getModulesWithLessonsForCourse(c.getCourseId()));
                }
                result.add(dto);
            }
        }
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allCourses", allEntries = true),
            @CacheEvict(value = "instructorCourses", allEntries = true),
            @CacheEvict(value = "verifiedCourses", allEntries = true),
            @CacheEvict(value = "courseDetails", key = "#courseId"),
            @CacheEvict(value = "liveCourseDetails", key = "#courseId"),
            @CacheEvict(value = "coursesByCategory", allEntries = true)
    })
    public String updateCourse(String token, String courseId, CourseEntity request, MultipartFile thumbnail) {
        if (request == null) {
            throw new NullException(NULL_BODY_MESSAGE);
        }
        CourseEntity existing = getCourseOrThrow(courseId);
        TrainerResponseDTO trainer = trainerClient.getTrainer(token);
        String ownerId = trainer.getUserId();

        if (!ownerId.equals(existing.getCreatorId())) {
            throw new ForbiddenException("Forbidden.");
        }

        applyThumbnailUpdate(existing, request.getThumbnailKey(), thumbnail, token, ownerId, courseId);
        applyBasicFieldUpdates(existing, request);

        existing.setUpdatedAt(Instant.now());
        courseRepository.save(existing);
        notifyAdminEmitters();
        return "Course Updated Successfully.";
    }

    private void applyThumbnailUpdate(CourseEntity existing, String thumbnailKey, MultipartFile thumbnail,
                                      String token, String ownerId, String courseId) {
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String newKey = uploadService.uploadFileDirectly(token, thumbnail, FileType.THUMBNAIL, courseId);
            existing.setThumbnailKey(newKey);
        } else if (thumbnailKey != null && !thumbnailKey.isEmpty() && !thumbnailKey.equals(existing.getThumbnailKey())) {
            uploadService.validateAndConsumeUpload(thumbnailKey, ownerId, courseId);
            existing.setThumbnailKey(thumbnailKey);
        }
    }

    private void applyBasicFieldUpdates(CourseEntity existing, CourseEntity request) {
        if (request.getTitle() != null) {
            existing.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            existing.setCategoryId(request.getCategoryId());
            existing.setCategoryName(fetchCategoryName(request.getCategoryId()));
        }
        if (request.getLanguage() != null) {
            existing.setLanguage(request.getLanguage());
        }
        if (request.getSkillLevel() != null) {
            existing.setSkillLevel(request.getSkillLevel());
        }
        if (request.getPrice() != null) {
            existing.setPrice(request.getPrice());
        }
        if (request.getWelcomeMessage() != null) {
            existing.setWelcomeMessage(request.getWelcomeMessage());
        }
        if (request.getCategory() != null) {
            existing.setCategory(request.getCategory());
        }
        if (request.getSubCategory() != null) {
            existing.setSubCategory(request.getSubCategory());
        }
        if (request.getTopic() != null) {
            existing.setTopic(request.getTopic());
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allCourses", allEntries = true),
            @CacheEvict(value = "instructorCourses", allEntries = true),
            @CacheEvict(value = "verifiedCourses", allEntries = true),
            @CacheEvict(value = "courseDetails", key = "#courseId"),
            @CacheEvict(value = "liveCourseDetails", key = "#courseId"),
            @CacheEvict(value = "coursesByCategory", allEntries = true)
    })
    public String verifyCourse(String token, String courseId) {
        log.info("Verifying course: courseId={}", courseId);
        CourseEntity course = getCourseOrThrow(courseId);
        course.setIsVerified(true);
        course.setStatus("VERIFIED");
        String adminId = jwtUtil.extractUserId(token);
        course.setVerifierId(adminId);
        courseRepository.save(course);
        notifyAdminEmitters();
        log.info("Course verified successfully: courseId={}, verifiedBy={}, title='{}'", courseId, adminId, course.getTitle());

        publishNotification(course.getCreatorId(), "Course Approved",
                "Your course \"" + course.getTitle() + "\" has been approved and is now available to learners.",
                NotificationType.COURSE_APPROVED, null, courseId);

        return "Course Verified Successfully.";
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "allCourses", allEntries = true),
            @CacheEvict(value = "instructorCourses", allEntries = true),
            @CacheEvict(value = "verifiedCourses", allEntries = true),
            @CacheEvict(value = "courseDetails", key = "#courseId"),
            @CacheEvict(value = "liveCourseDetails", key = "#courseId"),
            @CacheEvict(value = "coursesByCategory", allEntries = true)
    })
    public String rejectCourse(String token, String courseId, String remarks) {
        log.info("Rejecting course: courseId={}", courseId);
        CourseEntity course = getCourseOrThrow(courseId);
        course.setIsVerified(false);
        course.setStatus("REJECTED");
        String adminId = jwtUtil.extractUserId(token);
        course.setVerifierId(adminId);
        courseRepository.save(course);
        notifyAdminEmitters();
        log.info("Course rejected successfully: courseId={}, rejectedBy={}, title='{}'", courseId, adminId, course.getTitle());

        String reason = (remarks != null && !remarks.isBlank()) ? remarks : "Admin rejected.";
        publishNotification(course.getCreatorId(), "Course Rejected",
                "Your course \"" + course.getTitle() + "\" was rejected. Please review the feedback and update it. Reason: " + reason,
                NotificationType.COURSE_REJECTED, null, courseId);

        return "Course Rejected.";
    }

    private void publishNotification(String userId, String title, String message, NotificationType type,
                                     String referenceType, String referenceId) {
        try {
            NotificationRequest notifReq = NotificationRequest.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .type(type)
                    .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .build();
            notificationPublisher.publish(notifReq);
            log.info("{} notification sent for userId: {}", type, userId);
        } catch (Exception e) {
            log.error("Failed to send {} notification for userId {}", type, userId, e);
        }
    }

    @Override
    public SseEmitter streamAllCoursesAdmin() {
        log.info("Client subscribed to admin courses stream");
        SseEmitter emitter = new SseEmitter(0L);
        adminEmitters.add(emitter);

        Runnable removeEmitter = () -> adminEmitters.remove(emitter);
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError(throwable -> removeEmitter.run());

        try {
            emitter.send(getAllCoursesForAdmin());
        } catch (IOException e) {
            log.warn("Failed to send initial admin course snapshot; removing subscriber", e);
            removeEmitter.run();
        }
        return emitter;
    }

    @Override
    public List<CourseResponseDTO> getAllCoursesForAdmin() {
        List<CourseResponseDTO> result = new ArrayList<>();
        for (CourseEntity c : courseRepository.findAll()) {
            if (c != null) {
                CourseResponseDTO dto = toDTO(c);
                if (dto != null) {
                    dto.setModules(getModulesWithLessonsForCourse(c.getCourseId()));
                    result.add(dto);
                }
            }
        }
        return result;
    }

    private void notifyAdminEmitters() {
        if (adminEmitters.isEmpty()) {
            return;
        }
        List<CourseResponseDTO> allCourses = getAllCoursesForAdmin();
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : adminEmitters) {
            try {
                emitter.send(allCourses);
            } catch (IOException e) {
                log.warn("Failed to push update to an admin SSE subscriber; removing it", e);
                deadEmitters.add(emitter);
            }
        }
        adminEmitters.removeAll(deadEmitters);
    }

    @Override
    public String moderateCourse(String token, String courseId, CourseModerationRequest request) {
        if (request == null || request.getAction() == null || request.getAction().isBlank()) {
            log.warn("Moderation request missing action for courseId={}", courseId);
            throw new IllegalArgumentException("Action is required. Use 'VERIFY' or 'REJECT'.");
        }

        String action = request.getAction().toUpperCase(Locale.ROOT).trim();
        log.info("Moderating course: courseId={}, action={}, remarks={}", courseId, action, request.getRemarks());

        return switch (action) {
            case "VERIFY" -> getSelf().verifyCourse(token, courseId);
            case "REJECT" -> getSelf().rejectCourse(token, courseId, request.getRemarks());
            default -> {
                log.warn("Invalid moderation action '{}' for courseId={}", action, courseId);
                throw new IllegalArgumentException("Invalid action: " + action + ". Use 'VERIFY' or 'REJECT'.");
            }
        };
    }

    @Override
    @Cacheable(value = "verifiedCourses", key = "'reviewed'")
    public List<CourseResponseDTO> getAllReviewedCourses(String token) {
        List<CourseResponseDTO> result = new ArrayList<>();
        for (CourseEntity c : courseRepository.findAll()) {
            if ("VERIFIED".equalsIgnoreCase(c.getStatus()) || "REJECTED".equalsIgnoreCase(c.getStatus())) {
                CourseResponseDTO dto = toDTO(c);
                dto.setModules(getModulesWithLessonsForCourse(c.getCourseId()));
                result.add(dto);
            }
        }
        return result;
    }

    @Override
    @Cacheable(value = "courseDetails", key = "#courseId")
    public CourseResponseDTO getCourseDetail(String courseId) {
        CourseEntity course = getCourseOrThrow(courseId);
        String categoryName = fetchCategoryNameSilently(course.getCategoryId());
        List<ReviewResponseDTO> reviews = buildSortedReviewDTOs(courseId);
        int enrolledCount = fetchEnrolledCount(courseId);

        CourseResponseDTO dto = toDTO(course);
        if (dto != null) {
            if (categoryName != null) {
                dto.setCategoryName(categoryName);
            }
            dto.setReviews(reviews);
            dto.setTotalReviews(reviews.size());
            dto.setEnrolledCount(enrolledCount);
        }
        return dto;
    }

    private String fetchCategoryNameSilently(String categoryId) {
        try {
            CategoryEntity category = categoryRepository.findById(categoryId);
            return category != null ? category.getName() : null;
        } catch (Exception e) {
            log.warn("Could not fetch category: {}", e.getMessage());
            return null;
        }
    }

    private List<ReviewResponseDTO> buildSortedReviewDTOs(String courseId) {
        List<ReviewEntity> reviewEntities = new ArrayList<>(reviewRepository.findByCourseId(courseId));
        reviewEntities.sort(Comparator.comparing(
                (ReviewEntity r) -> r.getCreatedAt() != null ? r.getCreatedAt() : Instant.EPOCH,
                Comparator.reverseOrder()));

        List<ReviewResponseDTO> reviews = new ArrayList<>();
        for (ReviewEntity r : reviewEntities) {
            reviews.add(ReviewResponseDTO.builder()
                    .reviewId(r.getReviewId()).courseId(r.getCourseId()).userId(r.getUserId())
                    .reviewerName(r.getReviewerName()).reviewerPhoto(r.getReviewerPhoto())
                    .likes(r.getLikes()).createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                    .build());
        }
        return reviews;
    }

    private int fetchEnrolledCount(String courseId) {
        try {
            return enrollmentClient.getEnrolledLearners(courseId).size();
        } catch (Exception e) {
            log.warn("Could not fetch enrolled learners: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public CourseResponseDTO getCourseDetailWithModules(String courseId) {
        CourseResponseDTO cachedDto = getSelf().getCourseDetail(courseId);
        CourseResponseDTO dto = cachedDto.toBuilder().build();
        dto.setModules(getModulesWithLessonsForCourse(courseId));
        return dto;
    }

    @Override
    @Cacheable(value = "coursesByCategory", key = "#categoryId")
    public List<CourseResponseDTO> getCoursesByCategory(String categoryId) {
        List<CourseResponseDTO> result = new ArrayList<>();
        for (CourseEntity c : courseRepository.findByCategoryId(categoryId)) {
            result.add(toDTO(c));
        }
        return result;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "courseDetails", key = "#courseId"),
            @CacheEvict(value = "liveCourseDetails", key = "#courseId"),
            @CacheEvict(value = "allCourses", allEntries = true),
            @CacheEvict(value = "instructorCourses", allEntries = true),
            @CacheEvict(value = "verifiedCourses", allEntries = true),
            @CacheEvict(value = "coursesByCategory", allEntries = true)
    })
    public void refreshRatingCache(String courseId) {
        CourseEntity course = getCourseOrThrow(courseId);
        List<ReviewEntity> all = reviewRepository.findByCourseId(courseId);
        if (all.isEmpty()) {
            return;
        }
        double sum = 0.0;
        for (ReviewEntity r : all) {
            sum += (r.getRating() == null ? 0 : r.getRating());
        }
        double avg = sum / all.size();
        course.setAverageRating(Math.round(avg * 10.0) / 10.0);
        course.setTotalReviews(all.size());
        courseRepository.save(course);
        notifyAdminEmitters();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "courseDetails", key = "#courseId"),
            @CacheEvict(value = "liveCourseDetails", key = "#courseId"),
            @CacheEvict(value = "allCourses", allEntries = true),
            @CacheEvict(value = "instructorCourses", allEntries = true),
            @CacheEvict(value = "verifiedCourses", allEntries = true),
            @CacheEvict(value = "coursesByCategory", allEntries = true)
    })
    public void refreshCourseStats(String courseId) {
        CourseEntity course = getCourseOrThrow(courseId);
        List<ModuleEntity> modules = moduleRepository.findByCourseId(courseId);
        int totalLessons = 0;
        int totalMinutes = 0;
        for (ModuleEntity module : modules) {
            for (LessonEntity lesson : lessonRepository.findByModuleId(module.getModuleId())) {
                totalLessons++;
                totalMinutes += (lesson.getDuration() == null ? 0 : lesson.getDuration());
            }
        }
        course.setLessonCount(totalLessons);
        course.setDurationLabel(formatMinutes(totalMinutes));
        courseRepository.save(course);
        notifyAdminEmitters();
    }

    @Override
    public List<CourseResponseDTO> getEnrolledCoursesForStudent(String token) {
        if (token == null || token.isBlank()) {
            return Collections.emptyList();
        }
        String userId = jwtUtil.extractUserId(token);
        log.info("Fetching enrolled courses for student userId: {}", userId);

        List<UserEnrollmentResponse> userEnrollments = fetchEnrollmentsSafely(token, userId);
        if (userEnrollments.isEmpty()) {
            return Collections.emptyList();
        }

        List<CourseResponseDTO> enrolledCourses = new ArrayList<>();
        for (UserEnrollmentResponse enrollment : userEnrollments) {
            if (isInvalidOrInactive(enrollment)) {
                continue;
            }
            fetchAndAddCourse(enrollment.getTargetId(), enrolledCourses);
        }
        return enrolledCourses;
    }

    private List<UserEnrollmentResponse> fetchEnrollmentsSafely(String token, String userId) {
        try {
            List<UserEnrollmentResponse> enrollments = enrollmentClient.getUserEnrollmentsInternal(token);
            return enrollments != null ? enrollments : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch enrollments for userId: {}", userId, e);
            return Collections.emptyList();
        }
    }

    private boolean isInvalidOrInactive(UserEnrollmentResponse enrollment) {
        if (enrollment == null || enrollment.getTargetId() == null) {
            return true;
        }
        return enrollment.getStatus() != null && !"ACTIVE".equalsIgnoreCase(enrollment.getStatus());
    }

    private void fetchAndAddCourse(String courseId, List<CourseResponseDTO> enrolledCourses) {
        try {
            CourseEntity course = courseRepository.findById(courseId);
            if (course != null) {
                enrolledCourses.add(getCourseDetailWithModules(course.getCourseId()));
            }
        } catch (Exception e) {
            log.warn("Failed to load course details for enrolled targetId: {}", courseId, e);
        }
    }

    private CourseEntity getCourseOrThrow(String courseId) {
        CourseEntity course = courseRepository.findById(courseId);
        if (course == null) {
            throw new CourseNotFoundException(courseId);
        }
        return course;
    }

    private String formatMinutes(int total) {
        if (total <= 0) {
            return ZERO_MINUTES;
        }
        int h = total / 60;
        int m = total % 60;
        if (h > 0 && m > 0) {
            return h + "h " + m + "m";
        }
        if (h > 0) {
            return h + " Hour" + (h > 1 ? "s" : "");
        }
        return m + " Minute" + (m > 1 ? "s" : "");
    }

    private CourseResponseDTO toDTO(CourseEntity e) {
        if (e == null) {
            return null;
        }

        String instructorName = e.getCreatorName();
        String instructorPhoto = null;
        String instructorTitle = null;
        String instructorBio = null;

        try {
            TrainerResponseDTO trainer = trainerClient.getTrainerById(e.getCreatorId());
            if (trainer != null) {
                instructorName = trainer.getFullName();
                instructorPhoto = trainer.getProfilePictureURL();
                instructorTitle = trainer.getDesignation();
                instructorBio = trainer.getTrainingSpecialization();
            }
        } catch (Exception ex) {
            log.warn("Could not fetch instructor details for creatorId {}: {}", e.getCreatorId(), ex.getMessage());
        }

        return CourseResponseDTO.builder()
                .courseId(e.getCourseId())
                .title(e.getTitle())
                .description(e.getDescription())
                .status(e.getStatus())
                .isVerified(e.getIsVerified())
                .creatorId(e.getCreatorId())
                .creatorName(e.getCreatorName())
                .categoryId(e.getCategoryId())
                .categoryName(e.getCategoryName())
                .category(e.getCategory())
                .subCategory(e.getSubCategory())
                .topic(e.getTopic())
                .language(e.getLanguage())
                .skillLevel(e.getSkillLevel())
                .price(e.getPrice())
                .lessonCount(e.getLessonCount())
                .durationLabel(e.getDurationLabel())
                .averageRating(e.getAverageRating())
                .totalReviews(e.getTotalReviews())
                .welcomeMessage(e.getWelcomeMessage())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .thumbnailUrl(e.getThumbnailKey() == null ? null : fileService.generateDownloadUrl(e.getThumbnailKey()))
                .demoVideoUrl(e.getDemoVideoKey() == null ? null : fileService.generateDownloadUrl(e.getDemoVideoKey()))
                .courseType(e.getCourseType())
                .verifierId(e.getVerifierId())
                .instructorName(instructorName)
                .instructorPhoto(instructorPhoto)
                .instructorTitle(instructorTitle)
                .instructorBio(instructorBio)
                .build();
    }

    @Override
    public MultipartUploadInitResponse initiateDemoVideoMultipartUpload(String token, MultipartUploadInitRequest request) {
        UploadInitRequest newReq = new UploadInitRequest();
        newReq.setFileType(FileType.DEMO_VIDEO);
        newReq.setFileName(request.getFileName());
        newReq.setContentType(request.getContentType());
        newReq.setFileSize(request.getFileSize());
        UploadInitResponse res = uploadService.initiateMultipartUpload(token, newReq);
        return new MultipartUploadInitResponse(res.getUploadId(), res.getFileKey(), res.getMessage());
    }

    @Override
    public String generateDemoVideoPresignedUrl(String token, String uploadId, String fileKey, int partNumber) {
        UploadPresignedUrlRequest req = new UploadPresignedUrlRequest();
        req.setUploadId(uploadId);
        req.setPartNumber(partNumber);
        return uploadService.generatePresignedUrl(token, req);
    }

    @Override
    public CompleteMultipartUploadResponse completeDemoVideoMultipartUpload(String token, CompleteMultipartUploadRequestDTO request) {
        UploadCompleteRequest req = new UploadCompleteRequest();
        req.setUploadId(request.getUploadId());
        List<MultipartUploadPartETag> parts = new ArrayList<>();
        for (CompleteMultipartUploadRequestDTO.PartETag p : request.getParts()) {
            parts.add(new MultipartUploadPartETag(p.getPartNumber(), p.getETag()));
        }
        req.setParts(parts);
        return uploadService.completeMultipartUpload(token, req);
    }

    @Override
    public String abortDemoVideoMultipartUpload(String token, AbortMultipartUploadRequestDTO request) {
        UploadAbortRequest req = new UploadAbortRequest();
        req.setUploadId(request.getUploadId());
        uploadService.abortMultipartUpload(token, req);
        return "Upload aborted";
    }

    private List<ModuleResponseDTO> getModulesWithLessonsForCourse(String courseId) {
        List<ModuleEntity> modules = new ArrayList<>(moduleRepository.findByCourseId(courseId));
        modules.sort(Comparator.comparingInt(m -> m.getOrderIndex() == null ? 0 : m.getOrderIndex()));

        List<ModuleResponseDTO> result = new ArrayList<>();
        for (ModuleEntity m : modules) {
            List<LessonResponseDTO> lessonDTOs = buildLessonDTOs(m.getModuleId());
            result.add(ModuleResponseDTO.builder()
                    .moduleId(m.getModuleId())
                    .courseId(m.getCourseId())
                    .title(m.getTitle())
                    .description(m.getDescription())
                    .orderIndex(m.getOrderIndex())
                    .lessonCount(lessonDTOs.size())
                    .lessons(lessonDTOs)
                    .createdAt(m.getCreatedAt())
                    .updatedAt(m.getUpdatedAt())
                    .build());
        }
        return result;
    }

    private List<LessonResponseDTO> buildLessonDTOs(String moduleId) {
        List<LessonEntity> lessons = new ArrayList<>(lessonRepository.findByModuleId(moduleId));
        lessons.sort(Comparator.comparingInt(l -> l.getOrderIndex() == null ? 0 : l.getOrderIndex()));

        List<LessonResponseDTO> lessonDTOs = new ArrayList<>();
        for (LessonEntity l : lessons) {
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
        return lessonDTOs;
    }
}