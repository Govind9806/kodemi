package com.example.payment_service.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PayoutRequestDto {
    private BigDecimal amount;
    private String bankAccount;
    private String ifscCode;
    private String accountHolderName;
}