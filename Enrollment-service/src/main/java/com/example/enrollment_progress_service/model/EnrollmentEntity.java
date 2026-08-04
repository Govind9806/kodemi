package com.example.enrollment_progress_service.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.enrollment_progress_service.enums.EnrollmentStatus;
import com.example.enrollment_progress_service.enums.EnrollmentTargetType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@DynamoDBTable(tableName = "Enrollment")
public class EnrollmentEntity {

    @DynamoDBHashKey(attributeName = "enrollmentId")
    private String enrollmentId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "user-index", attributeName = "userId")
    private String userId;

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "user-index", attributeName = "creatorId")
    @DynamoDBAttribute(attributeName = "creatorId")
    private String creatorId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "target-index", attributeName = "targetId")
    private String targetId;

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "targetType")
    private EnrollmentTargetType targetType;

    @DynamoDBAttribute(attributeName = "paymentId")
    private String paymentId;

    @DynamoDBAttribute(attributeName = "orderId")
    private String orderId;

    @DynamoDBAttribute(attributeName = "paidAt")
    private String paidAt;

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "status")
    private EnrollmentStatus status;

    @DynamoDBAttribute(attributeName = "enrolledAt")
    private String enrolledAt;

    @DynamoDBAttribute(attributeName = "unenrolledAt")
    private String unenrolledAt;

    @DynamoDBAttribute(attributeName = "createdAt")
    private String createdAt;

    @DynamoDBAttribute(attributeName = "updatedAt")
    private String updatedAt;
}
