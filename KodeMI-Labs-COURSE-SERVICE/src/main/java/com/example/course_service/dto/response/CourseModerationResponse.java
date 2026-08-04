package com.example.course_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseModerationResponse {

    private String message;
    private String courseId;
    private String action;
    private Boolean isVerified;
    private List<CourseResponseDTO> courses;
}
