package com.example.admin_service.dto.request;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DynamoDBTable(tableName = "payout_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutRequest {

    @DynamoDBHashKey(attributeName = "payoutId")
    private String payoutId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "trainerId-index")
    private String trainerId;

    @DynamoDBAttribute
    private BigDecimal amount;

    @DynamoDBAttribute
    private String bankAccount;

    @DynamoDBAttribute
    private String ifscCode;

    @DynamoDBAttribute
    private String accountHolderName;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "status-index")
    private String status;

    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    @DynamoDBIndexRangeKey(
            globalSecondaryIndexNames = {
                    "trainerId-index",
                    "status-index"
            }
    )
    private LocalDateTime requestedAt;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime processedAt;

    @DynamoDBAttribute
    private String processedBy;

    @DynamoDBAttribute
    private String remarks;
}