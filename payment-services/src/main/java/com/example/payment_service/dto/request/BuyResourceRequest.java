package com.example.payment_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyResourceRequest {
    private String userId;
    private String resourceId;
    private String resourceName;
    private BigDecimal amount;
    private String trainerId;
    private String targetType;
    private String resourceType;
    private String courseType;

    public String getEffectiveTargetType() {
        if (targetType != null && !targetType.isBlank()) return targetType;
        if (resourceType != null && !resourceType.isBlank()) return resourceType;
        if (courseType != null && !courseType.isBlank()) return courseType;
        return "RECORDED_COURSE";
    }
}