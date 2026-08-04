package com.example.course_service.service;

import com.example.course_service.dto.response.ReviewResponseDTO;
import com.example.course_service.model.ReviewEntity;
import java.util.List;

public interface ReviewService {

    String createReview(ReviewEntity request, String token);
    ReviewResponseDTO getReviewById(String reviewId);
    List<ReviewResponseDTO> getAllReviews();
    List<ReviewResponseDTO> findByCourseId(String courseId);
    String updateReview(String token, String reviewId, ReviewEntity review);
}