package com.example.course_service.service.impl;

import com.example.course_service.dto.notification.NotificationChannel;
import com.example.course_service.dto.notification.NotificationRequest;
import com.example.course_service.dto.notification.NotificationType;
import com.example.course_service.dto.response.AccessCheckResponse;
import com.example.course_service.dto.response.CourseResponseDTO;
import com.example.course_service.dto.response.ReviewResponseDTO;
import com.example.course_service.exception.ForbiddenException;
import com.example.course_service.exception.NullException;
import com.example.course_service.exception.ReviewNotFoundException;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.model.ReviewEntity;
import com.example.course_service.repository.ReviewRepository;
import com.example.course_service.service.CourseService;
import com.example.course_service.service.ReviewService;
import com.example.course_service.service.notification.NotificationPublisher;
import com.example.course_service.util.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private static final String COURSE_TYPE_RECORDED = "RECORDED";
    private static final String COURSE_TYPE_LIVE = "LIVE";
    private static final String COURSE_TYPE_RECORDED_COURSE = "RECORDED_COURSE";
    private static final String COURSE_TYPE_LIVE_COURSE = "LIVE_COURSE";

    private static final String CACHE_COURSE_REVIEWS = "courseReviews";
    private static final String CACHE_COURSE_DETAILS = "courseDetails";
    private static final String CACHE_LIVE_COURSE_DETAILS = "liveCourseDetails";
    private static final String CACHE_ALL_COURSES = "allCourses";
    private static final String CACHE_VERIFIED_COURSES = "verifiedCourses";
    private static final String CACHE_COURSES_BY_CATEGORY = "coursesByCategory";

    private final CacheManager cacheManager;
    private final ReviewRepository reviewRepository;
    private final CourseService courseService;
    private final EnrollmentClient enrollmentClient;
    private final JwtUtil jwtUtil;
    private final NotificationPublisher notificationPublisher;

    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             @Lazy CourseService courseService,
                             EnrollmentClient enrollmentClient,
                             JwtUtil jwtUtil,
                             NotificationPublisher notificationPublisher,
                             @Autowired(required = false) CacheManager cacheManager) {
        this.reviewRepository = reviewRepository;
        this.courseService = courseService;
        this.enrollmentClient = enrollmentClient;
        this.jwtUtil = jwtUtil;
        this.notificationPublisher = notificationPublisher;
        this.cacheManager = cacheManager;
    }

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             @Lazy CourseService courseService,
                             EnrollmentClient enrollmentClient,
                             JwtUtil jwtUtil,
                             NotificationPublisher notificationPublisher) {
        this(reviewRepository, courseService, enrollmentClient, jwtUtil, notificationPublisher, null);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CACHE_COURSE_REVIEWS, key = "#request.courseId"),
            @CacheEvict(value = CACHE_COURSE_DETAILS, key = "#request.courseId"),
            @CacheEvict(value = CACHE_LIVE_COURSE_DETAILS, key = "#request.courseId"),
            @CacheEvict(value = CACHE_ALL_COURSES, allEntries = true),
            @CacheEvict(value = CACHE_VERIFIED_COURSES, allEntries = true),
            @CacheEvict(value = CACHE_COURSES_BY_CATEGORY, allEntries = true)
    })
    public String createReview(ReviewEntity request, String token) {
        if (request == null) {
            throw new NullException("Null Body.");
        }
        log.info("Creating review for courseId: {}", request.getCourseId());

        String userId = jwtUtil.extractUserId(token);
        String reviewerName = jwtUtil.extractName(token);

        CourseResponseDTO courseDetail = fetchCourseDetail(request.getCourseId());
        String courseType = resolveCourseType(courseDetail);

        assertUserHasAccess(userId, request.getCourseId(), courseType);
        assertNotAlreadyReviewed(userId, request.getCourseId());

        ReviewEntity entity = buildReviewEntity(request, userId, reviewerName);
        reviewRepository.save(entity);
        courseService.refreshRatingCache(request.getCourseId());
        log.info("Review created successfully for courseId: {}", request.getCourseId());

        notifyCourseCreator(courseDetail, request.getCourseId());

        return "Review Created Successfully.";
    }

    private CourseResponseDTO fetchCourseDetail(String courseId) {
        try {
            return courseService.getCourseDetail(courseId);
        } catch (RuntimeException e) {
            log.warn("Unable to fetch course detail for courseId: {}. Falling back to default course type.",
                    courseId, e);
            return null;
        }
    }

    private String resolveCourseType(CourseResponseDTO courseDetail) {
        if (courseDetail == null || courseDetail.getCourseType() == null) {
            return COURSE_TYPE_RECORDED_COURSE;
        }
        String type = courseDetail.getCourseType();
        if (COURSE_TYPE_RECORDED.equalsIgnoreCase(type)) {
            return COURSE_TYPE_RECORDED_COURSE;
        }
        if (COURSE_TYPE_LIVE.equalsIgnoreCase(type)) {
            return COURSE_TYPE_LIVE_COURSE;
        }
        return type;
    }

    private void assertUserHasAccess(String userId, String courseId, String courseType) {
        AccessCheckResponse accessCheck = enrollmentClient.checkAccess(userId, courseId, courseType);
        boolean enrolled = accessCheck != null && accessCheck.isHasAccess();
        if (!enrolled) {
            throw new ForbiddenException("You must be enrolled in this course to write a review.");
        }
    }

    private void assertNotAlreadyReviewed(String userId, String courseId) {
        boolean alreadyReviewed = reviewRepository.findByCourseId(courseId).stream()
                .anyMatch(r -> userId.equals(r.getUserId()));
        if (alreadyReviewed) {
            throw new IllegalArgumentException("You have already reviewed this course.");
        }
    }

    private ReviewEntity buildReviewEntity(ReviewEntity request, String userId, String reviewerName) {
        ReviewEntity entity = new ReviewEntity();
        entity.setReviewId(UUID.randomUUID().toString());
        entity.setCourseId(request.getCourseId());
        entity.setUserId(userId);
        entity.setReviewerName(reviewerName);
        entity.setReviewerPhoto(request.getReviewerPhoto());
        entity.setRating(request.getRating());
        entity.setReviewText(request.getReviewText());
        entity.setIsVerified(false);
        entity.setLikes(0);
        Date now = new Date();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private void notifyCourseCreator(CourseResponseDTO courseDetail, String courseId) {
        if (courseDetail == null || courseDetail.getCreatorId() == null) {
            return;
        }
        try {
            NotificationRequest notifReq = NotificationRequest.builder()
                    .title("New Course Review")
                    .message("A new review has been added to your course \"" + courseDetail.getTitle() + "\".")
                    .type(NotificationType.COURSE_REVIEW_ADDED)
                    .channels(List.of(NotificationChannel.IN_APP))
                    .userId(courseDetail.getCreatorId())
                    .referenceId(courseId)
                    .referenceType("COURSE")
                    .build();
            notificationPublisher.publish(notifReq);
        } catch (RuntimeException e) {
            log.error("Failed to notify trainer about new review for courseId: {}", courseId, e);
        }
    }

    @Override
    public ReviewResponseDTO getReviewById(String reviewId) {
        log.debug("Fetching review by id: {}", reviewId);
        return toDTO(getReviewOrThrow(reviewId));
    }

    @Override
    public List<ReviewResponseDTO> getAllReviews() {
        List<ReviewResponseDTO> result = new ArrayList<>();
        for (ReviewEntity r : reviewRepository.findAll()) {
            result.add(toDTO(r));
        }
        log.debug("Fetched {} reviews.", result.size());
        return result;
    }

    @Override
    @Cacheable(value = CACHE_COURSE_REVIEWS, key = "#courseId")
    public List<ReviewResponseDTO> findByCourseId(String courseId) {
        List<ReviewEntity> list = new ArrayList<>(reviewRepository.findByCourseId(courseId));
        list.sort(Comparator.comparing(ReviewEntity::getCreatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());

        List<ReviewResponseDTO> result = new ArrayList<>();
        for (ReviewEntity r : list) {
            result.add(toDTO(r));
        }
        log.debug("Found {} reviews for courseId: {}", result.size(), courseId);
        return result;
    }

    @Override
    @Transactional
    public String updateReview(String token, String reviewId, ReviewEntity request) {
        if (request == null) {
            throw new NullException("Null Body.");
        }
        String userId = jwtUtil.extractUserId(token);
        ReviewEntity existing = getReviewOrThrow(reviewId);

        if (!userId.equals(existing.getUserId())) {
            log.warn("User {} forbidden from updating review {} (owned by {})", userId, reviewId, existing.getUserId());
            throw new ForbiddenException("You can only edit your own review.");
        }

        if (request.getRating() != null) {
            existing.setRating(request.getRating());
        }
        if (request.getReviewText() != null) {
            existing.setReviewText(request.getReviewText());
        }
        existing.setUpdatedAt(new Date());
        reviewRepository.save(existing);
        courseService.refreshRatingCache(existing.getCourseId());
        log.info("Review {} updated successfully.", reviewId);

        evictReviewCaches(existing.getCourseId());

        return "Review Updated Successfully.";
    }

    private void evictReviewCaches(String courseId) {
        if (cacheManager == null || courseId == null) {
            return;
        }
        evictKey(CACHE_COURSE_REVIEWS, courseId);
        evictKey(CACHE_COURSE_DETAILS, courseId);
        evictKey(CACHE_LIVE_COURSE_DETAILS, courseId);
        clearCache(CACHE_ALL_COURSES);
        clearCache(CACHE_VERIFIED_COURSES);
        clearCache(CACHE_COURSES_BY_CATEGORY);
    }

    private void evictKey(String cacheName, String key) {
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

    private ReviewEntity getReviewOrThrow(String reviewId) {
        ReviewEntity review = reviewRepository.findById(reviewId);
        if (review == null) {
            log.warn("Review not found for reviewId: {}", reviewId);
            throw new ReviewNotFoundException(reviewId);
        }
        return review;
    }

    private ReviewResponseDTO toDTO(ReviewEntity e) {
        if (e == null) {
            return null;
        }
        return ReviewResponseDTO.builder()
                .reviewId(e.getReviewId())
                .courseId(e.getCourseId())
                .userId(e.getUserId())
                .reviewerName(e.getReviewerName())
                .reviewerPhoto(e.getReviewerPhoto())
                .rating(e.getRating())
                .reviewText(e.getReviewText())
                .isVerified(e.getIsVerified())
                .likes(e.getLikes())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}