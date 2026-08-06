package com.example.course_service.service;

import com.example.course_service.dto.response.ReviewResponseDTO;
import com.example.course_service.exception.ForbiddenException;
import com.example.course_service.exception.NullException;
import com.example.course_service.exception.ReviewNotFoundException;
import com.example.course_service.feign.EnrollmentClient;
import com.example.course_service.model.ReviewEntity;
import com.example.course_service.repository.ReviewRepository;
import com.example.course_service.service.impl.ReviewServiceImpl;
import com.example.course_service.service.notification.NotificationPublisher;
import com.example.course_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewServiceImplTest {

    private ReviewServiceImpl reviewService;
    private ReviewRepository reviewRepository;
    private CourseService courseService;
    private EnrollmentClient enrollmentClient;
    private JwtUtil jwtUtil;
    private NotificationPublisher notificationPublisher;

    private ReviewEntity review;
    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setup() {
        reviewRepository = mock(ReviewRepository.class);
        courseService = mock(CourseService.class);
        enrollmentClient = mock(EnrollmentClient.class);
        jwtUtil = mock(JwtUtil.class);
        notificationPublisher = mock(NotificationPublisher.class);
        reviewService = new ReviewServiceImpl(reviewRepository, courseService, enrollmentClient, jwtUtil, notificationPublisher);

        review = new ReviewEntity();
        review.setReviewId("review-1");
        review.setCourseId("course-1");
        review.setUserId("user-1");
        review.setReviewerName("Test User");
        review.setRating(5);
        review.setReviewText("Excellent course");
        review.setIsVerified(false);
        review.setLikes(0);
        review.setCreatedAt(java.time.Instant.ofEpochMilli(1000000000000L));
        review.setUpdatedAt(java.time.Instant.ofEpochMilli(1000000000000L));
    }

    @Test
    void createReview_Success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(jwtUtil.extractName(TOKEN)).thenReturn("Test User");
        when(enrollmentClient.checkAccess(anyString(), anyString(), anyString())).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(true).build());
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of());

        String response = reviewService.createReview(review, TOKEN);

        assertEquals("Review Created Successfully.", response);

        ArgumentCaptor<ReviewEntity> captor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(reviewRepository, times(1)).save(captor.capture());

        ReviewEntity saved = captor.getValue();
        assertNotNull(saved.getReviewId());
        assertEquals("course-1", saved.getCourseId());
        assertEquals("user-1", saved.getUserId());
        assertEquals(5, saved.getRating());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void createReview_NullBody_ThrowsException() {
        assertThrows(NullException.class, () -> reviewService.createReview(null, TOKEN));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_NotEnrolled_ThrowsForbidden() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(jwtUtil.extractName(TOKEN)).thenReturn("Test User");
        when(enrollmentClient.checkAccess(anyString(), anyString(), anyString())).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(false).build());

        assertThrows(ForbiddenException.class, () -> reviewService.createReview(review, TOKEN));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_AlreadyReviewed_ThrowsException() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(jwtUtil.extractName(TOKEN)).thenReturn("Test User");
        when(enrollmentClient.checkAccess(anyString(), anyString(), anyString())).thenReturn(com.example.course_service.dto.response.AccessCheckResponse.builder().hasAccess(true).build());
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of(review));

        assertThrows(IllegalArgumentException.class, () -> reviewService.createReview(review, TOKEN));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void getReviewById_Found_ReturnsDTO() {
        when(reviewRepository.findById("review-1")).thenReturn(review);

        ReviewResponseDTO dto = reviewService.getReviewById("review-1");

        assertNotNull(dto);
        assertEquals("review-1", dto.getReviewId());
        assertEquals(5, dto.getRating());
    }

    @Test
    void getReviewById_NotFound_ThrowsException() {
        when(reviewRepository.findById("review-1")).thenReturn(null);

        assertThrows(ReviewNotFoundException.class, () -> reviewService.getReviewById("review-1"));
    }

    @Test
    void getAllReviews_ReturnsMappedList() {
        when(reviewRepository.findAll()).thenReturn(List.of(review));

        List<ReviewResponseDTO> list = reviewService.getAllReviews();

        assertEquals(1, list.size());
        assertEquals("Excellent course", list.get(0).getReviewText());
    }

    @Test
    void findByCourseId_ReturnsMappedList() {
        when(reviewRepository.findByCourseId("course-1")).thenReturn(List.of(review));

        List<ReviewResponseDTO> list = reviewService.findByCourseId("course-1");

        assertEquals(1, list.size());
        assertEquals("course-1", list.get(0).getCourseId());
    }

    @Test
    void updateReview_Success() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(reviewRepository.findById("review-1")).thenReturn(review);

        ReviewEntity updateRequest = new ReviewEntity();
        updateRequest.setRating(3);
        updateRequest.setReviewText("Updated review");

        String response = reviewService.updateReview(TOKEN, "review-1", updateRequest);

        assertEquals("Review Updated Successfully.", response);
        verify(reviewRepository, times(1)).save(review);
        assertEquals(3, review.getRating());
        assertEquals("Updated review", review.getReviewText());
    }

    @Test
    void updateReview_NotOwner_ThrowsForbidden() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-999");
        when(reviewRepository.findById("review-1")).thenReturn(review);

        ReviewEntity updateRequest = new ReviewEntity();
        updateRequest.setRating(1);

        assertThrows(ForbiddenException.class,
                () -> reviewService.updateReview(TOKEN, "review-1", updateRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_NullBody_ThrowsException() {
        assertThrows(NullException.class,
                () -> reviewService.updateReview(TOKEN, "review-1", null));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_NotFound_ThrowsException() {
        when(jwtUtil.extractUserId(TOKEN)).thenReturn("user-1");
        when(reviewRepository.findById("review-1")).thenReturn(null);

        assertThrows(ReviewNotFoundException.class,
                () -> reviewService.updateReview(TOKEN, "review-1", review));
    }
}
