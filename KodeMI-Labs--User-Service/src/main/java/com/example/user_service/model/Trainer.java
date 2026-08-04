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
import java.util.List;

@DynamoDBTable(tableName = "Trainer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trainer{
    @DynamoDBHashKey(attributeName = "userId")
    private String userId;

    @DynamoDBAttribute
    private String fullName;

    @DynamoDBAttribute
    private String gender;

    @DynamoDBAttribute
    private String designation;

    @DynamoDBAttribute
    private String phoneNumber;

    @DynamoDBAttribute
    private String email;

    @DynamoDBAttribute
    private Float ratingValue;

    @DynamoDBAttribute
    private List<String> languageKnown;

    @DynamoDBAttribute
    private String demoContentKey;

    @DynamoDBAttribute
    private String resumeKey;

    @DynamoDBAttribute
    private String officeName;

    @DynamoDBAttribute
    private String officeAddress;

    @DynamoDBAttribute
    private String linkedInOrWebsiteURL;

    @DynamoDBAttribute
    private String gitHubUrl;

    @DynamoDBAttribute
    private String trainingSpecialization;

    @DynamoDBAttribute
    private Integer yearsOfExperience;

    @DynamoDBAttribute
    private String qualification;

    @DynamoDBAttribute
    private String modesOfTrainingPreferred;

    @DynamoDBAttribute
    private String clientsTrainedBefore;

    @DynamoDBAttribute
    private String profileImageURL;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt;

    @DynamoDBAttribute
    private String panNumber;

    @DynamoDBTypeConverted(converter = LocalDateConverter.class)
    @DynamoDBAttribute
    private LocalDate dateOfBirth;

    @DynamoDBAttribute
    private String bankName;

    @DynamoDBAttribute
    private String branchName;

    @DynamoDBAttribute
    private String accountNumber;

    @DynamoDBAttribute
    private String ifscCode;

    @DynamoDBAttribute
    private String globalCertifications;

    @DynamoDBAttribute
    private String topRegistration;

    @DynamoDBAttribute
    private String supportingDocumentsChecklist;

    @DynamoDBAttribute
    private Boolean anyLegalDisputesInPast5Years;
}
