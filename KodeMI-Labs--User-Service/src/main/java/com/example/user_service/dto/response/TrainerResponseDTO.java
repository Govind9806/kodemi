package com.example.user_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerResponseDTO {

    private String userId;

    private String fullName;

    private String designation;

    private String phoneNumber;

    private String emailId;
    private List<String> languageKnown;
    private String officeName;
    private String contentUrl;
    private String resumeUrl;
    private Float rating;

    private String officeAddress;

    private String linkedInOrWebsiteURL;

    private String githubURL;

    private String trainingSpecialization;

    private Integer yearsOfExperience;

    private String qualification;

    private String modesOfTrainingPreferred;

    private String clientsTrainedBefore;

    private String profilePictureURL;

    private LocalDate dateOfBirth;

    private LocalDateTime updatedAt;


    private LocalDateTime createdAt;

    private String gender;

    private String globalCertifications;

    private String topRegistration;

    private boolean accountStatus;

    private String supportingDocumentsChecklist;

    private Boolean anyLegalDisputesInPast5Years;

    private Boolean isVerified;
    private String status;
    private String rejectionReason;
}

