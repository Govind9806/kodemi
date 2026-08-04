package com.example.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDto {
    private String transactionId;
    private String userId;
    private String transactionType;
    private BigDecimal amount;
    private String description;
    private String status;
    private String createdAt;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
}