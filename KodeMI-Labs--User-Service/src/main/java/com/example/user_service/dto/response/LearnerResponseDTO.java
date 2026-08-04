package com.example.user_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnerResponseDTO {

    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String profilePictureUrl;
    private LocalDate dateOfBirth;
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

    private Boolean emailVerified;
    private Boolean accountStatus;
    private LocalDateTime updatedAt;
}
