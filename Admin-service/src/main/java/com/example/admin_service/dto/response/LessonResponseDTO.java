package com.example.admin_service.dto.response;


import com.example.admin_service.dto.request.LessonEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
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

    // LIVE-lesson specific fields
    private String lessonType;
    private String liveSessionId;
    private Date scheduledAt;

    private List<LessonEntity.ContentItem> contentKey;
    private Date createdAt;
    private Date updatedAt;

    public void maskContent() {
        this.contentKey = null;
        this.videoKey = null; // also hide the raw key for non-enrolled users
    }
}