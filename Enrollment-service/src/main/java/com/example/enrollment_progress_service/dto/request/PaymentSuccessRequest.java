package com.example.enrollment_progress_service.dto.request;

import com.example.enrollment_progress_service.enums.EnrollmentTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessRequest {

    @NotBlank(message = "paymentId is required")
    private String paymentId;

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "targetId is required")
    private String targetId;

    @NotNull(message = "targetType is required")
    private EnrollmentTargetType targetType;

    @NotBlank(message = "status is required")
    private String status;

}
