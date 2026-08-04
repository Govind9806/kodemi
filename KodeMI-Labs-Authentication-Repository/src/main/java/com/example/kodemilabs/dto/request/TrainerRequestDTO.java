package com.example.kodemilabs.dto.request;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerRequestDTO {
    private String fullName;
    private String designation;
    private String phoneNumber;
    private String email;
    private String gender;
    private String officeName;
    private String officeAddress;
    private String linkedInOrWebsiteURL;
    private String linkedinUrl;
    private String githubUrl;
    private String trainingSpecialization;
    private Integer yearsOfExperience;
    private String qualification;
    private LocalDate dateOfBirth;
    private String modesOfTrainingPreferred;
    private String clientsTrainedBefore;
    private String profilePictureURL;
    private String panNumber;
    private String bankName;
    private String branchName;
    private String accountNumber;
    private String ifscCode;
    private String globalCertifications;
    private String topRegistration;
    private String supportingDocumentsChecklist;
    private Boolean anyLegalDisputesInPast5Years;
}