package com.example.kodemilabs.model;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@DynamoDBTable(tableName = "LoginHistory")
@NoArgsConstructor
@Setter
public class LoginHistory {

    private String userId;
    private Instant loginAttemptTime;
    private String loginStatus;
    private String ipAddress;
    private String userAgent;

    @DynamoDBHashKey(attributeName = "userId")
    public String getUserId() {
        return userId;
    }

    @DynamoDBRangeKey(attributeName = "loginAttemptTime")
    @DynamoDBTypeConvertedTimestamp
    public Instant getLoginAttemptTime() {
        return loginAttemptTime;
    }

    @DynamoDBAttribute(attributeName = "loginStatus")
    public String getLoginStatus() {
        return loginStatus;
    }

    @DynamoDBAttribute(attributeName = "ipAddress")
    public String getIpAddress() {
        return ipAddress;
    }

    @DynamoDBAttribute(attributeName = "userAgent")
    public String getUserAgent() {
        return userAgent;
    }
}
