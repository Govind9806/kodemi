package com.example.enrollment_progress_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationResponse {
    private boolean paid;
    private String paymentId;
    private String orderId;
    private String status;
    private BigDecimal amount;
}
