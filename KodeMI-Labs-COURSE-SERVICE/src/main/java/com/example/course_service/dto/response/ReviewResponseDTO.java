package com.example.course_service.dto.response;

import lombok.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {
    private String reviewId;
    private String courseId;
    private String userId;
    private String reviewerName;
    private String reviewerPhoto;
    private Integer rating;
    private String reviewText;
    private Boolean isVerified;
    private Integer likes;
    private Date createdAt;
    private Date updatedAt;
}