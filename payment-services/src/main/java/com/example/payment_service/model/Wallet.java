package com.example.payment_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DynamoDBTable(tableName = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    // Primary key
    @DynamoDBHashKey(attributeName = "userId")
    private String userId;

    // GSI: query wallets by user type
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "userType-index")
    private String userType;

    @DynamoDBAttribute
    private BigDecimal balance;

    @DynamoDBAttribute
    private BigDecimal totalEarned;

    @DynamoDBAttribute
    private BigDecimal totalSpent;

    @DynamoDBAttribute
    private BigDecimal pendingPayout;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt;

    @DynamoDBAttribute
    private String status;

}