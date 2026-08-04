package com.example.user_service.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TrainerAdminResponse {
    private String userId;

    private String fullName;

    private String gender;

    private String designation;

    private String phoneNumber;

    private String email;

    private Float ratingValue;

    private List<String> languageKnown;

    private String demoContentKey;

    private String resumeKey;

    private String officeName;

    private String officeAddress;

    private String linkedInOrWebsiteURL;

    private String gitHubUrl;

    private String trainingSpecialization;

    private Integer yearsOfExperience;

    private String qualification;

    private String modesOfTrainingPreferred;

    private String clientsTrainedBefore;

    private String profileImageURL;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String panNumber;

    private LocalDate dateOfBirth;

    private String bankName;

    private String branchName;

    private String accountNumber;

    private String ifscCode;

    private String globalCertifications;

    private String topRegistration;

    private String supportingDocumentsChecklist;

    private Boolean anyLegalDisputesInPast5Years;
}
