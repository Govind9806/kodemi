package com.example.admin_service.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDTO {
    private String courseId;
    private String creatorId;
    private String creatorName;
    private String categoryId;
    private String categoryName;

    private Set<String> category;
    private Set<String> subCategory;
    private Set<String> topic;

    private String status;
    private Boolean isVerified;
    private String title;
    private String description;
    private String language;
    private String skillLevel;
    private String thumbnailUrl;
    private String demoVideoUrl;
    private Double price;
    private Integer lessonCount;
    private String durationLabel;
    private Double averageRating;
    private Integer totalReviews;
    private String welcomeMessage;
    private String instructorName;
    private String instructorPhoto;
    private String instructorTitle;
    private String instructorBio;
    private String courseType;
    private Date createdAt;
    private Date updatedAt;
    private List<ReviewResponseDTO> reviews;
    private List<LessonResponseDTO> lessons;

    public void maskVideoContent() {
        if (this.lessons != null) {
            for (LessonResponseDTO lesson : this.lessons) {
                if (lesson != null) {
                    lesson.maskContent();
                }
            }
        }
    }
}
