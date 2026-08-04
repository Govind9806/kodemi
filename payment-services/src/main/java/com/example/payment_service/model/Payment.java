package com.example.payment_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DynamoDBTable(tableName = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @DynamoDBHashKey(attributeName = "orderId")
    private String orderId;

    @DynamoDBAttribute
    private String paymentId;

    // GSI for querying payments by user
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "userId-index")
    private String userId;

    @DynamoDBAttribute
    private String targetId;

    @DynamoDBAttribute
    private String targetType;

    @DynamoDBAttribute
    private BigDecimal amount;

    @DynamoDBAttribute
    private String currency;

    @DynamoDBAttribute
    private String status;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt;
}