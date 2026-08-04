package com.example.course_service.dto.response;

import lombok.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@Builder(toBuilder = true)
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
    private String verifierId;
    private Date createdAt;
    private Date updatedAt;
    private List<ReviewResponseDTO> reviews;
    private List<LessonResponseDTO> lessons;
    private List<ModuleResponseDTO> modules;
    private Integer enrolledCount;

    public void maskVideoContent() {
        if (this.lessons != null) {
            for (LessonResponseDTO lesson : this.lessons) {
                lesson.maskContent();
            }
        }
        if (this.modules != null) {
            for (ModuleResponseDTO module : this.modules) {
                if (module.getLessons() != null) {
                    for (LessonResponseDTO lesson : module.getLessons()) {
                        lesson.maskContent();
                    }
                }
            }
        }
    }
}