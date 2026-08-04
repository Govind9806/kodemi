package com.example.admin_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

@Data
@DynamoDBTable(tableName = "AdminRateLimit")
public class AdminRateLimit {

    @DynamoDBHashKey(attributeName = "adminId_date")
    private String adminIdDate;

    @DynamoDBAttribute(attributeName = "broadcastCount")
    private int broadcastCount;
}
