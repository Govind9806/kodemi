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
public class TrainerUserResponseDTO {

    private String userId;
    private String fullName;
    private String designation;
    private String phoneNumber;
    private String emailId;
    private List<String> languageKnown;
    private String officeName;
    private String contentUrl;
    private Float rating;
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
    private String gender;
    private String globalCertifications;
    private String topRegistration;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
