package com.example.admin_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

import com.example.admin_service.dto.request.LocalDateTimeConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DynamoDBTable(tableName = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @DynamoDBHashKey(attributeName = "transactionId")
    private String transactionId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "userId-index")
    private String userId;

    @DynamoDBAttribute
    private String transactionType;

    @DynamoDBAttribute
    private BigDecimal amount;

    @DynamoDBAttribute
    private String description;

    @DynamoDBAttribute
    private String referenceId;

    @DynamoDBAttribute
    private String razorpayPaymentId;

    @DynamoDBAttribute
    private String razorpayOrderId;

    @DynamoDBAttribute
    private String status;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt;

    @DynamoDBAttribute
    private BigDecimal balanceBefore;

    @DynamoDBAttribute
    private BigDecimal balanceAfter;


}