package com.example.payment_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.payment_service.enums.SubscriptionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@DynamoDBTable(tableName = "Subscription")
public class AiSubscribe {

    @DynamoDBHashKey
    private String subscriptionId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "user-index")
    private String userId;

    @DynamoDBAttribute
    private String planId;

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute
    private SubscriptionStatus status;

    @DynamoDBAttribute
    private Boolean autoRenew;

    @DynamoDBAttribute
    private String paymentId;

    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    @DynamoDBAttribute
    private LocalDateTime startDate;

    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    @DynamoDBAttribute
    private LocalDateTime endDate;

    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    @DynamoDBAttribute
    private LocalDateTime createdAt;

    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    @DynamoDBAttribute
    private LocalDateTime updatedAt;
}
