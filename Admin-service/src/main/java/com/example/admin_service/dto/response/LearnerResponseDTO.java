package com.example.admin_service.dto.response;

import lombok.*;

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

    private Boolean emailVerified;
    private Boolean accountStatus;
    private LocalDateTime updatedAt;
}
