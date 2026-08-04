package com.example.enrollment_progress_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import com.example.enrollment_progress_service.enums.EnrollmentStatus;
import com.example.enrollment_progress_service.enums.EnrollmentTargetType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private String message;
    private String enrollmentId;
    private String creatorId;
    private String targetId;
    private EnrollmentTargetType targetType;
    private EnrollmentStatus status;
    private boolean paymentRequired;
    private String paymentId;
    private BigDecimal amount;
}
