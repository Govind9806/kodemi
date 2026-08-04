package com.example.course_service.controller;

import com.example.course_service.dto.response.ReviewResponseDTO;
import com.example.course_service.model.ReviewEntity;
import com.example.course_service.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private static final String MESSAGE = "message";
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createReview(
            @RequestHeader("Authorization") String token,
            @RequestBody ReviewEntity request) {
        return ResponseEntity.ok(Map.of(MESSAGE, reviewService.createReview(request, token)));
    }

    @PutMapping("/edit/{reviewId}")
    public ResponseEntity<Map<String, String>> updateReview(
            @RequestHeader("Authorization") String token,
            @PathVariable String reviewId,
            @RequestBody ReviewEntity request) {
        return ResponseEntity.ok(Map.of(MESSAGE, reviewService.updateReview(token, reviewId, request)));
    }

    @GetMapping("/get/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> getReviewById(
            @PathVariable String reviewId) {
        return ResponseEntity.ok(reviewService.getReviewById(reviewId));
    }

    @GetMapping("/get/all")
    public ResponseEntity<List<ReviewResponseDTO>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByCourse(
            @PathVariable String courseId) {
        return ResponseEntity.ok(reviewService.findByCourseId(courseId));
    }
}