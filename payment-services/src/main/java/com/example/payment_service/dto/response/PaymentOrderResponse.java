package com.example.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentOrderResponse {
    @JsonProperty("id")
    @JsonAlias({"order_id", "orderId"})
    private String orderId;
    
    private BigDecimal amount;
    
    @JsonProperty("amount_paise")
    private long amountPaise;
    
    private String currency;
    private String status;

    private String paymentLink;
    private String paymentUrlId;
}
