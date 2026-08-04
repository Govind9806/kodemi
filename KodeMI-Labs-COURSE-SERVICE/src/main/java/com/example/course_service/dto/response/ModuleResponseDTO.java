package com.example.course_service.dto.response;

import lombok.*;
import java.util.Date;
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
    private Date createdAt;
    private Date updatedAt;
    private List<LessonResponseDTO> lessons; // null unless tapped
}