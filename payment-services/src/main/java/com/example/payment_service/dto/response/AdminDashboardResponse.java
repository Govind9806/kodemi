package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private BigDecimal totalRevenue;
    private BigDecimal totalPayouts;
    private BigDecimal pendingPayouts;
    private BigDecimal totalRefunds;
    private int totalTransactions;
    private int activeLearners;
    private int activeTrainers;
}