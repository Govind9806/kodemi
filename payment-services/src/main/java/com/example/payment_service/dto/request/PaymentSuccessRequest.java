package com.example.payment_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessRequest {
    private String paymentId;
    private String orderId;
    private String userId;
    private String targetId;
    private String targetType;
    private String status;
}
