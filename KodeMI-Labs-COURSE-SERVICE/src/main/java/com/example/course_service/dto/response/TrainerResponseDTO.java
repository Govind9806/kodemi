package com.example.course_service.dto.response;

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
    private String modesOfTrainingPreferred;
    private String clientsTrainedBefore;
    private String profilePictureURL;
    private LocalDate dateOfBirth;
    private String globalCertifications;
    private String topRegistration;
    private String supportingDocumentsChecklist;
    private Boolean anyLegalDisputesInPast5Years;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}