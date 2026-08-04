package com.example.payment_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddFundsRequest {
    private String userId;
    private BigDecimal amount;
    private String currency = "INR";
    private String returnUrl;
}