package com.example.admin_service.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private String officeName;
    private String contentUrl;
    private String resumeUrl;
    private String rating;

    private String officeAddress;

    private String linkedInURL;

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
}

