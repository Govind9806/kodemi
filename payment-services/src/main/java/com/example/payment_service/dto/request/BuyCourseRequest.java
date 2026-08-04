package com.example.payment_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyCourseRequest {
    private String userId;
    private String courseId;
    private String courseName;
    private BigDecimal amount;
    private String trainerId;
    private String targetType;
    private String courseType;

    public String getEffectiveTargetType() {
        if (targetType != null && !targetType.isBlank()) return targetType;
        if (courseType != null && !courseType.isBlank()) return courseType;
        return "RECORDED_COURSE";
    }
}