package com.example.enrollment_progress_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEnrollmentStatsResponse {
    private String targetId;
    private String creatorId;
    private String targetType;
    private long activeCount;
    private long cancelledCount;
    private long paymentRequiredCount;
    private long expiredCount;
    private long totalCount;
}
