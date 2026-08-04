package com.example.enrollment_progress_service.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.enrollment_progress_service.enums.EnrollmentStatus;
import com.example.enrollment_progress_service.enums.EnrollmentTargetType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatusResponse {
    private boolean enrolled;
    private EnrollmentStatus status;

    @JsonAlias({"targetId", "insertId", "id"})
    @JsonProperty("targetId")
    private String targetId;

    private String creatorId;

    @JsonAlias({"targetType", "insertType"})
    @JsonProperty("targetType")
    private EnrollmentTargetType targetType;

    @JsonProperty("insertId")
    public String getInsertId() {
        return targetId;
    }

    @JsonProperty("courseId")
    public String getCourseId() {
        return (targetType == EnrollmentTargetType.RECORDED_COURSE || targetType == EnrollmentTargetType.LIVE_COURSE) ? targetId : null;
    }

    @JsonProperty("insertType")
    public String getInsertType() {
        return targetType != null ? targetType.name() : null;
    }
}
