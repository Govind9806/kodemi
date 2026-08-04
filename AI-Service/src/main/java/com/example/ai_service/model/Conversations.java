package com.example.ai_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@DynamoDBTable(tableName = "Conversation")
public class Conversations {
    @DynamoDBHashKey
    private String conversationId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "user-index")
    private String userId;

    @DynamoDBAttribute
    private String botType;

    @DynamoDBAttribute
    private String sessionId;

    @DynamoDBAttribute
    private String conversationName;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt;

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "user-index")
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime lastMessageAt;
}
