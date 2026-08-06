package com.example.course_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveCourseDetailResponseDTO {
    private String courseId;
    private String title;
    private String description;
    private String categoryId;
    private String categoryName;
    private String category;
    private String subCategory;
    private String topic;
    private String creatorId;
    private String creatorName;
    private String language;
    private String skillLevel;
    private Double price;
    private String thumbnailUrl;
    private String demoVideoUrl;
    private String status;
    private Boolean isVerified;
    private Double averageRating;
    private Integer totalReviews;
    private String welcomeMessage;
    private String congratsMessage;
    private String courseType; // RECORDED or LIVE
    private Instant createdAt;
    private Instant updatedAt;
    
    // LIVE-specific fields
    private String liveSessionId;
    private Instant sessionStartTime;
    private Instant sessionEndTime;
    private String joinLink;
    private String sessionStatus;
    private String recordingUrl;
    
    // Instructor details
    private String instructorName;
    private String instructorTitle;
    private String instructorBio;
    private String instructorPhoto;
    
    // Reviews
    private List<ReviewResponseDTO> reviews;

    public void maskVideoContent() {
        this.joinLink = null;
        this.recordingUrl = null;
        this.liveSessionId = null;
    }
}

