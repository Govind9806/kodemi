package com.example.course_service.dto.response;

import com.example.course_service.model.LessonEntity;
import lombok.Data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponseDTO {

    private String lessonId;
    private String courseId;
    private String moduleId;
    private String title;
    private String description;
    private Integer duration;
    private Integer orderIndex;
    private String videoKey;
    private String videoUrl;

    // LIVE-lesson specific fields
    private String lessonType;
    private String liveSessionId;
    private Instant scheduledAt;

    private List<LessonEntity.ContentItem> contentKey;
    private Instant createdAt;
    private Instant updatedAt;

    public void maskContent() {
        this.contentKey = null;
        this.videoKey = null; // also hide the raw key for non-enrolled users
        this.videoUrl = null;
    }
}