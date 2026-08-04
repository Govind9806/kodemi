package com.example.payment_service.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaymentVerifyRequest {
    @JsonProperty("razorpay_order_id")
    @JsonAlias("razorpayOrderId")
    private String razorpayOrderId;

    @JsonProperty("razorpay_payment_id")
    @JsonAlias("razorpayPaymentId")
    private String razorpayPaymentId;

    @JsonProperty("razorpay_signature")
    @JsonAlias("razorpaySignature")
    private String razorpaySignature;

}