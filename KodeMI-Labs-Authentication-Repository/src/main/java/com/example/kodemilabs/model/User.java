package com.example.kodemilabs.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.kodemilabs.enums.Role;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DynamoDBTable(tableName = "user")
@Setter
@NoArgsConstructor
public class User {

    private String userId;
    private String name;
    private String email;
    private String username;
    private String passwordHash;
    private boolean active;
    private boolean verified;
    private Long lastLogin;
    private Role role;
    private String status;
    private Long lockedUntil;
    private String verifierId;

    @DynamoDBHashKey(attributeName = "userId")
    public String getUserId() {
        return userId;
    }

    @DynamoDBAttribute
    public String getName() {
        return name;
    }

    @DynamoDBIndexHashKey(
            globalSecondaryIndexName = "email-index",
            attributeName = "email"
    )
    public String getEmail() {
        return email;
    }

    @DynamoDBIndexHashKey(
            globalSecondaryIndexName = "username-index",
            attributeName = "username"
    )
    public String getUsername() {
        return username;
    }

    @DynamoDBAttribute
    public String getPasswordHash() {
        return passwordHash;
    }

    @DynamoDBAttribute
    public boolean isActive() {
        return active;
    }

    @DynamoDBAttribute
    public boolean isVerified() {
        return verified;
    }

    @DynamoDBAttribute
    public Long getLastLogin() {
        return lastLogin;
    }

    @DynamoDBTypeConvertedEnum
    @DynamoDBIndexHashKey(
            globalSecondaryIndexName = "role-status-index",
            attributeName = "role"
    )
    public Role getRole() {
        return role;
    }


    @DynamoDBIndexRangeKey(
            globalSecondaryIndexName = "role-status-index",
            attributeName = "status"
    )
    public String getStatus() { return status; }

    @DynamoDBAttribute
    public Long getLockedUntil() { return lockedUntil; }

    @DynamoDBAttribute
    public String getVerifierId() { return verifierId; }
}
