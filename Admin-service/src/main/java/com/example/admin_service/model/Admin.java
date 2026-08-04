package com.example.admin_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.admin_service.enums.AdminRole;
import lombok.*;

@DynamoDBTable(tableName = "Admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

    @DynamoDBHashKey(attributeName = "adminId")
    private String adminId;
    @DynamoDBAttribute
    private String password;
    @DynamoDBIndexHashKey(
            globalSecondaryIndexName = "email-index"
    )
    private String email;
    @DynamoDBAttribute
    private String username;
    @DynamoDBTypeConvertedEnum
    @DynamoDBIndexHashKey(
            globalSecondaryIndexName = "adminRole-index"
    )
    private AdminRole adminRole;
    @DynamoDBAttribute
    private boolean pending;

}
