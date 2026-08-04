package com.example.user_service.dto.request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnerRequestDTO {

    private String userId;

    private String username;

    @Email(message = "Email should be valid")
    private String email;

    private String fullName;

    @Pattern(regexp = "^$|^[+]?\\d{10,15}$", message = "Phone number must be valid (10-15 digits)")
    private String phoneNumber;

    @PastOrPresent(message = "Date of birth cannot be in the future")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^$|^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
    private String gender;

    private String linkedinUrl;

    private String githubUrl;

    private String selectedSkill;
    private String preferredSkill;
    private String profession;
    private String skillLevel;
    private String skillPriority;
    private String learningGoal;
    private java.util.List<String> topics;

    private boolean accountStatus;
}
