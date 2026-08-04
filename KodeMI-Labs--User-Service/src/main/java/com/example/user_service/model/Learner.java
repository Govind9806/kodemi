package com.example.user_service.model;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@DynamoDBTable(tableName = "Learner")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Learner {

    @DynamoDBHashKey(attributeName = "userId")
    private String userId;

    @DynamoDBAttribute
    private String username;

    @DynamoDBAttribute(attributeName = "email")
    private String email;

    @DynamoDBAttribute
    private String fullName;

    @DynamoDBAttribute(attributeName = "phoneNumber")
    private String phoneNumber;

    @DynamoDBAttribute(attributeName = "profilePictureUrl")
    private String profilePictureKey;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateConverter.class)
    private LocalDate dateOfBirth;

    @DynamoDBAttribute
    private String gender;

    @DynamoDBAttribute
    private Boolean emailVerified;

    @DynamoDBAttribute
    private Boolean phoneVerified;

    @DynamoDBAttribute
    private boolean accountStatus;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt;

    @DynamoDBAttribute
    private String linkedinUrl;

    @DynamoDBAttribute
    private String githubUrl;

    @DynamoDBAttribute
    private String selectedSkill;

    @DynamoDBAttribute
    private String preferredSkill;

    @DynamoDBAttribute
    private String profession;

    @DynamoDBAttribute
    private String skillLevel;

    @DynamoDBAttribute
    private String skillPriority;

    @DynamoDBAttribute
    private String learningGoal;

    @DynamoDBAttribute
    private java.util.List<String> topics;
}
