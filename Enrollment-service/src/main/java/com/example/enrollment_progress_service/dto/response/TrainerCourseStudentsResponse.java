package com.example.enrollment_progress_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerCourseStudentsResponse {
    private String courseId;
    private String courseTitle;
    private String targetType;
    private BigDecimal price;
    private int totalEnrolledStudents;
    private List<StudentDetailResponse> enrolledStudents;
    private int totalReviews;
    private Double averageRating;
    private List<ReviewResponseDTO> reviews;
}
