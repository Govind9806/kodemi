package com.example.ai_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.ai_service.enums.BotType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DynamoDBTable(tableName = "Message")
public class Messages {
    @DynamoDBHashKey
    private String messageId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "conversation-index")
    private String conversationId;

    @DynamoDBAttribute
    private String userId;

    @DynamoDBAttribute
    private String content;

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute
    private BotType botType;

    /**
     * Stored as ISO-8601 String (e.g. "2025-07-21T11:46:43") so DynamoDB can
     * sort it correctly as the GSI range key on "conversation-index".
     * Combining @DynamoDBIndexRangeKey + @DynamoDBTypeConverted(LocalDateTime)
     * causes a ValidationException: DynamoDB mistakes createdAt for the hash key.
     * Convert to/from LocalDateTime in the service layer instead.
     */
    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "conversation-index")
    private String createdAt;
}
