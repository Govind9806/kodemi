package com.example.payment_service.dto.request;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@NoArgsConstructor
public class PaymentOrderRequest {
    private String userid;
    private String targetId;
    private String targetType;
    private BigDecimal amount;
    private String currency;
}
