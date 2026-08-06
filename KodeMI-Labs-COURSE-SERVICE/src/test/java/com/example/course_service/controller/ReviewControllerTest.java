package com.example.course_service.controller;

import com.example.course_service.dto.response.ReviewResponseDTO;
import com.example.course_service.model.ReviewEntity;
import com.example.course_service.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewControllerTest {

    private ReviewController reviewController;
    private ReviewService reviewService;

    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void setup() {
        reviewService = mock(ReviewService.class);
        reviewController = new ReviewController(reviewService);
    }

    // ==================== createReview ====================
    @Test
    void createReview_CallsService() {
        ReviewEntity review = new ReviewEntity();
        review.setReviewText("Great course!");

        when(reviewService.createReview(review, TOKEN))
                .thenReturn("Review Created Successfully.");

        ResponseEntity<Map<String, String>> response =
                reviewController.createReview(TOKEN, review);

        verify(reviewService, times(1)).createReview(review, TOKEN);
        assertEquals("Review Created Successfully.", response.getBody().get("message"));
    }

    // ==================== getReviewById ====================
    @Test
    void getReviewById_ReturnsReview() {

        ReviewResponseDTO dto = ReviewResponseDTO.builder()
                .reviewId("R101")
                .courseId("C101")
                .userId("U101")
                .rating(5)
                .reviewText("Excellent course")
                .isVerified(false)
                .likes(0)
                .createdAt(Instant.ofEpochMilli(1000000000000L))
                .updatedAt(Instant.ofEpochMilli(1000000000000L))
                .build();

        when(reviewService.getReviewById("R101")).thenReturn(dto);

        ResponseEntity<ReviewResponseDTO> response =
                reviewController.getReviewById("R101");

        verify(reviewService, times(1)).getReviewById("R101");

        assertNotNull(response.getBody());
        assertEquals("R101", response.getBody().getReviewId());
        assertEquals("C101", response.getBody().getCourseId());
        assertEquals("U101", response.getBody().getUserId());
        assertEquals(5, response.getBody().getRating());
        assertEquals("Excellent course", response.getBody().getReviewText());
    }

    // ==================== getAllReviews ====================
    @Test
    void getAllReviews_ReturnsList() {

        ReviewResponseDTO dto1 = ReviewResponseDTO.builder()
                .reviewId("R101")
                .reviewText("Excellent course")
                .build();

        ReviewResponseDTO dto2 = ReviewResponseDTO.builder()
                .reviewId("R102")
                .reviewText("Good course")
                .build();

        List<ReviewResponseDTO> mockList = Arrays.asList(dto1, dto2);

        when(reviewService.getAllReviews()).thenReturn(mockList);

        ResponseEntity<List<ReviewResponseDTO>> response =
                reviewController.getAllReviews();

        verify(reviewService, times(1)).getAllReviews();

        assertEquals(2, response.getBody().size());
        assertEquals("R101", response.getBody().get(0).getReviewId());
        assertEquals("Good course", response.getBody().get(1).getReviewText());
    }

    // ==================== getReviewsByCourse ====================
    @Test
    void getReviewsByCourse_ReturnsList() {

        ReviewResponseDTO dto1 = ReviewResponseDTO.builder()
                .reviewId("R101")
                .courseId("C101")
                .reviewText("Excellent course")
                .build();

        ReviewResponseDTO dto2 = ReviewResponseDTO.builder()
                .reviewId("R102")
                .courseId("C101")
                .reviewText("Good course")
                .build();

        List<ReviewResponseDTO> mockList = Arrays.asList(dto1, dto2);

        when(reviewService.findByCourseId("C101")).thenReturn(mockList);

        ResponseEntity<List<ReviewResponseDTO>> response =
                reviewController.getReviewsByCourse("C101");

        verify(reviewService, times(1)).findByCourseId("C101");

        assertEquals(2, response.getBody().size());
        assertEquals("R101", response.getBody().get(0).getReviewId());
        assertEquals("R102", response.getBody().get(1).getReviewId());
    }

    // ==================== updateReview ====================
    @Test
    void updateReview_CallsService() {

        ReviewEntity review = new ReviewEntity();
        review.setReviewText("Updated review");

        when(reviewService.updateReview(TOKEN, "R101", review))
                .thenReturn("Review Updated Successfully.");

        ResponseEntity<Map<String, String>> response =
                reviewController.updateReview(TOKEN, "R101", review);

        verify(reviewService, times(1)).updateReview(TOKEN, "R101", review);

        assertEquals("Review Updated Successfully.", response.getBody().get("message"));
    }
}