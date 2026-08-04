package com.example.kodemilabs.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@DynamoDBTable(tableName = "RefreshTokens")
@Data
@NoArgsConstructor
public class RefreshToken {

    private String token;
    private String email;
    private Long expiry;
    private boolean invalidated;

    @DynamoDBHashKey(attributeName = "token")
    public String getToken() {
        return token;
    }

    @DynamoDBIndexHashKey(
            globalSecondaryIndexName = "email-index",
            attributeName = "email"
    )
    public String getEmail() {
        return email;
    }

    @DynamoDBAttribute(attributeName = "expiry")
    public Long getExpiry() {
        return expiry;
    }

    @DynamoDBAttribute(attributeName = "invalidated")
    public boolean isInvalidated() {
        return invalidated;
    }
}
