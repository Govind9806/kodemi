package com.example.course_service.service.impl;

import com.example.course_service.dto.request.AttachRecordingRequest;
import com.example.course_service.dto.request.LinkLiveSessionRequest;
import com.example.course_service.dto.response.LiveCourseDetailResponseDTO;
import com.example.course_service.dto.response.LiveSessionResponse;
import com.example.course_service.dto.response.ReviewResponseDTO;
import com.example.course_service.dto.response.TrainerResponseDTO;
import com.example.course_service.exception.CourseNotFoundException;
import com.example.course_service.feign.LiveClient;
import com.example.course_service.feign.TrainerClient;
import com.example.course_service.model.CategoryEntity;
import com.example.course_service.model.CourseEntity;
import com.example.course_service.model.LiveCourseMapping;
import com.example.course_service.model.ReviewEntity;
import com.example.course_service.repository.CategoryRepository;
import com.example.course_service.repository.CourseRepository;
import com.example.course_service.repository.LiveCourseMappingRepository;
import com.example.course_service.repository.ReviewRepository;
import com.example.course_service.service.FileService;
import com.example.course_service.service.LiveCourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LiveCourseServiceImpl implements LiveCourseService {

    private static final Logger log = LoggerFactory.getLogger(LiveCourseServiceImpl.class);

    private static final String COURSE_TYPE_MISMATCH_LOG = "Course type mismatch for courseId {}: type is {}";
    private static final String LIVE_COURSE_TYPE = "LIVE";

    private final CourseRepository courseRepository;
    private final LiveCourseMappingRepository liveMappingRepository;
    private final LiveClient liveClient;
    private final TrainerClient trainerClient;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final FileService fileService;

    public LiveCourseServiceImpl(CourseRepository courseRepository,
                                 LiveCourseMappingRepository liveMappingRepository,
                                 LiveClient liveClient,
                                 TrainerClient trainerClient,
                                 CategoryRepository categoryRepository,
                                 ReviewRepository reviewRepository,
                                 FileService fileService) {
        this.courseRepository = courseRepository;
        this.liveMappingRepository = liveMappingRepository;
        this.liveClient = liveClient;
        this.trainerClient = trainerClient;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
        this.fileService = fileService;
    }

    @Override
    @CacheEvict(value = "liveCourseDetails", key = "#courseId")
    public String linkLiveSession(String courseId, LinkLiveSessionRequest request) {
        log.info("Entering linkLiveSession for courseId: {}, liveSessionId: {}",
                courseId, request != null ? request.getLiveSessionId() : null);

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null.");
        }

        // Validate course exists and is LIVE type
        CourseEntity course = getCourseOrThrow(courseId);
        if (!LIVE_COURSE_TYPE.equalsIgnoreCase(course.getCourseType())) {
            log.warn(COURSE_TYPE_MISMATCH_LOG, courseId, course.getCourseType());
            throw new IllegalArgumentException("Course is not a LIVE type course.");
        }

        String liveSessionId = request.getLiveSessionId();
        if (liveSessionId == null || liveSessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("liveSessionId cannot be null or empty.");
        }

        // Validate live session exists in Live Service
        try {
            liveClient.getSessionById(liveSessionId);
        } catch (Exception e) {
            log.warn("Could not validate live session: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid liveSessionId: " + liveSessionId);
        }

        // Check if mapping already exists
        LiveCourseMapping existing = liveMappingRepository.findByCourseId(courseId);
        if (existing != null) {
            existing.setLiveSessionId(liveSessionId);
            existing.setUpdatedAt(Instant.now());
            liveMappingRepository.save(existing);
            log.info("Updated existing live course mapping for courseId: {}", courseId);
        } else {
            LiveCourseMapping mapping = new LiveCourseMapping();
            mapping.setId(UUID.randomUUID().toString());
            mapping.setCourseId(courseId);
            mapping.setLiveSessionId(liveSessionId);
            mapping.setCreatedAt(Instant.now());
            mapping.setUpdatedAt(Instant.now());
            liveMappingRepository.save(mapping);
            log.info("Created new live course mapping for courseId: {}", courseId);
        }

        log.info("Live session linked successfully for courseId: {}", courseId);
        return "Live session linked successfully.";
    }

    @Override
    @CacheEvict(value = "liveCourseDetails", key = "#request.courseId")
    public String attachRecording(AttachRecordingRequest request) {
        log.info("Entering attachRecording for courseId: {}, liveSessionId: {}",
                request != null ? request.getCourseId() : null,
                request != null ? request.getLiveSessionId() : null);

        if (request == null || request.getCourseId() == null || request.getLiveSessionId() == null || request.getRecordingUrl() == null) {
            throw new IllegalArgumentException("courseId, liveSessionId, and recordingUrl are required.");
        }

        // Validate course exists and is LIVE type
        CourseEntity course = getCourseOrThrow(request.getCourseId());
        if (!LIVE_COURSE_TYPE.equalsIgnoreCase(course.getCourseType())) {
            log.warn(COURSE_TYPE_MISMATCH_LOG, request.getCourseId(), course.getCourseType());
            throw new IllegalArgumentException("Course is not a LIVE type course.");
        }

        // Find or create mapping
        LiveCourseMapping mapping = liveMappingRepository.findByCourseId(request.getCourseId());
        if (mapping == null) {
            mapping = new LiveCourseMapping();
            mapping.setId(UUID.randomUUID().toString());
            mapping.setCourseId(request.getCourseId());
            mapping.setCreatedAt(Instant.now());
        }

        mapping.setLiveSessionId(request.getLiveSessionId());
        mapping.setRecordingUrl(request.getRecordingUrl());
        mapping.setUpdatedAt(Instant.now());
        liveMappingRepository.save(mapping);

        log.info("Recording attached successfully for courseId: {}", request.getCourseId());
        return "Recording attached successfully.";
    }

    @Override
    public LiveCourseDetailResponseDTO getLiveCourseDetail(String courseId) {
        log.info("Entering getLiveCourseDetail for courseId: {}", courseId);
        CourseEntity course = getCourseOrThrow(courseId);

        if (!LIVE_COURSE_TYPE.equalsIgnoreCase(course.getCourseType())) {
            log.warn(COURSE_TYPE_MISMATCH_LOG, courseId, course.getCourseType());
            throw new IllegalArgumentException("This course is not a LIVE course.");
        }

        LiveSessionData sessionData = fetchLiveSessionData(courseId);
        InstructorData instructorData = fetchInstructorData(course);
        String categoryName = fetchCategoryName(course.getCategoryId());
        List<ReviewResponseDTO> reviews = fetchSortedReviews(courseId);

        LiveCourseDetailResponseDTO dto = buildLiveCourseDetailResponse(course, sessionData, instructorData, categoryName, reviews);
        log.info("Exiting getLiveCourseDetail successfully for courseId: {}", courseId);
        return dto;
    }

    private LiveSessionData fetchLiveSessionData(String courseId) {
        LiveCourseMapping mapping = liveMappingRepository.findByCourseId(courseId);
        if (mapping == null) return new LiveSessionData(null, null);
        try {
            return new LiveSessionData(liveClient.getSessionById(mapping.getLiveSessionId()), mapping.getRecordingUrl());
        } catch (Exception e) {
            log.warn("Could not fetch live session details: {}", e.getMessage());
            return new LiveSessionData(null, null);
        }
    }

    private InstructorData fetchInstructorData(CourseEntity course) {
        try {
            TrainerResponseDTO trainer = trainerClient.getTrainerById(course.getCreatorId());
            return new InstructorData(trainer.getFullName(), trainer.getDesignation(),
                    trainer.getTrainingSpecialization(), trainer.getProfilePictureURL());
        } catch (Exception e) {
            log.warn("Could not fetch trainer for course {}: {}", course.getCourseId(), e.getMessage());
            return new InstructorData(course.getCreatorName(), null, null, null);
        }
    }

    private String fetchCategoryName(String categoryId) {
        try {
            CategoryEntity category = categoryRepository.findById(categoryId);
            return category != null ? category.getName() : null;
        } catch (Exception e) {
            log.warn("Could not fetch category: {}", e.getMessage());
            return null;
        }
    }

    private List<ReviewResponseDTO> fetchSortedReviews(String courseId) {
        List<ReviewEntity> list = new ArrayList<>(reviewRepository.findByCourseId(courseId));
        list.sort((a, b) -> {
            Instant da = a.getCreatedAt() == null ? Instant.EPOCH : a.getCreatedAt();
            Instant db = b.getCreatedAt() == null ? Instant.EPOCH : b.getCreatedAt();
            return db.compareTo(da);
        });

        List<ReviewResponseDTO> result = new ArrayList<>();
        for (ReviewEntity r : list) {
            result.add(ReviewResponseDTO.builder()
                    .reviewId(r.getReviewId()).courseId(r.getCourseId()).userId(r.getUserId())
                    .reviewerName(r.getReviewerName()).reviewerPhoto(r.getReviewerPhoto())
                    .rating(r.getRating()).reviewText(r.getReviewText())
                    .isVerified(r.getIsVerified()).likes(r.getLikes())
                    .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt()).build());
        }
        return result;
    }

    private LiveCourseDetailResponseDTO buildLiveCourseDetailResponse(
            CourseEntity course, LiveSessionData session, InstructorData instructor,
            String categoryName, List<ReviewResponseDTO> reviews) {
        LiveSessionResponse ls = session.liveSession();
        return LiveCourseDetailResponseDTO.builder()
                .courseId(course.getCourseId()).title(course.getTitle()).description(course.getDescription())
                .categoryId(course.getCategoryId()).categoryName(categoryName)
                .category(course.getCategory() != null ? course.getCategory().toString() : null)
                .subCategory(course.getSubCategory() != null ? course.getSubCategory().toString() : null)
                .topic(course.getTopic() != null ? course.getTopic().toString() : null)
                .creatorId(course.getCreatorId()).creatorName(course.getCreatorName())
                .language(course.getLanguage()).skillLevel(course.getSkillLevel()).price(course.getPrice())
                .thumbnailUrl(fileService.generatePresignedUrl(course.getThumbnailKey()))
                .demoVideoUrl(fileService.generatePresignedUrl(course.getDemoVideoKey()))
                .status(course.getStatus()).isVerified(course.getIsVerified())
                .averageRating(course.getAverageRating()).totalReviews(reviews.size())
                .welcomeMessage(course.getWelcomeMessage())
                .courseType(LIVE_COURSE_TYPE).createdAt(course.getCreatedAt()).updatedAt(course.getUpdatedAt())
                .liveSessionId(ls != null ? ls.getSessionId() : null)
                .sessionStartTime(ls != null ? ls.getStartTime() : null)
                .sessionEndTime(ls != null ? ls.getEndTime() : null)
                .joinLink(ls != null ? ls.getJoinLink() : null)
                .sessionStatus(ls != null ? ls.getStatus() : null)
                .recordingUrl(session.recordingUrl())
                .instructorName(instructor.name()).instructorTitle(instructor.title())
                .instructorBio(instructor.bio()).instructorPhoto(instructor.photo())
                .reviews(reviews).build();
    }

    private record LiveSessionData(LiveSessionResponse liveSession, String recordingUrl) {}
    private record InstructorData(String name, String title, String bio, String photo) {}

    private CourseEntity getCourseOrThrow(String courseId) {
        CourseEntity course = courseRepository.findById(courseId);
        if (course == null) throw new CourseNotFoundException(courseId);
        return course;
    }
}