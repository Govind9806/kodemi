package com.example.course_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEnrollmentResponse {
    private String enrollmentId;
    private String userId;
    private String targetId;
    private String targetType;
    private String status;
    private String enrolledAt;
}
