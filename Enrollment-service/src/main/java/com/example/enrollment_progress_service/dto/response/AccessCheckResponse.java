package com.example.enrollment_progress_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.enrollment_progress_service.enums.EnrollmentStatus;
import com.example.enrollment_progress_service.enums.EnrollmentTargetType;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessCheckResponse {
    @JsonProperty("hasAccess")
    private boolean hasAccess;
    private EnrollmentStatus status;
    private String targetId;
    private EnrollmentTargetType targetType;
}
