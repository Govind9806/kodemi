package com.example.enrollment_progress_service.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.enrollment_progress_service.enums.EnrollmentTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequest {

    @JsonProperty("targetId")
    @JsonAlias({"courseId", "targetId", "id", "conferenceId"})
    private String targetId;

    @JsonProperty("targetType")
    @JsonAlias({"type", "targetType", "courseType"})
    private EnrollmentTargetType targetType;

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public void setCourseId(String courseId) {
        if (this.targetId == null || this.targetId.isBlank()) {
            this.targetId = courseId;
        }
    }

    public void setId(String id) {
        if (this.targetId == null || this.targetId.isBlank()) {
            this.targetId = id;
        }
    }

    public EnrollmentTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(Object typeObj) {
        if (typeObj == null) return;
        if (typeObj instanceof EnrollmentTargetType) {
            this.targetType = (EnrollmentTargetType) typeObj;
        } else {
            String str = typeObj.toString().trim().toUpperCase();
            if ("RECORDED".equals(str) || "RECORDED_COURSE".equals(str)) {
                this.targetType = EnrollmentTargetType.RECORDED_COURSE;
            } else if ("LIVE".equals(str) || "LIVE_COURSE".equals(str)) {
                this.targetType = EnrollmentTargetType.LIVE_COURSE;
            } else if ("CONFERENCE".equals(str)) {
                this.targetType = EnrollmentTargetType.CONFERENCE;
            } else {
                try {
                    this.targetType = EnrollmentTargetType.valueOf(str);
                } catch (Exception e) {
                    this.targetType = EnrollmentTargetType.RECORDED_COURSE;
                }
            }
        }
    }

    public void setType(Object typeObj) {
        setTargetType(typeObj);
    }

    public void setCourseType(Object typeObj) {
        setTargetType(typeObj);
    }
}
