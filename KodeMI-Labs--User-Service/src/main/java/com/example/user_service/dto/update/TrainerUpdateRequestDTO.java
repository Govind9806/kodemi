package com.example.user_service.dto.update;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerUpdateRequestDTO {

    private String designation;

    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    private String officeName;

    private String officeAddress;

    private String linkedInOrWebsiteURL;

    private String trainingSpecialization;

    @Min(value = 0, message = "Years of experience cannot be negative")
    private Integer yearsOfExperience;

    private String qualification;

    private String modesOfTrainingPreferred;

    private String clientsTrainedBefore;

    private String profileImageURL;

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
