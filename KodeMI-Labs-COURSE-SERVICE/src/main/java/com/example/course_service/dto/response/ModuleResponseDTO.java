package com.example.course_service.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleResponseDTO {
    private String moduleId;
    private String courseId;
    private String title;
    private String description;
    private Integer orderIndex;
    private Integer lessonCount;
    private Instant createdAt;
    private Instant updatedAt;
    private List<LessonResponseDTO> lessons; // null unless tapped
}