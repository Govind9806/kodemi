package com.example.enrollment_progress_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDTO {
    private String courseId;
    private String title;
    private String creatorId;
    private String creatorName;
    private String categoryId;
    private String categoryName;
    private String status;
    private Boolean isVerified;
    private String description;
    private String language;
    private String skillLevel;
    private String thumbnailUrl;
    private BigDecimal price;
    private String courseType;
    private Double averageRating;
    private Integer totalReviews;
}
