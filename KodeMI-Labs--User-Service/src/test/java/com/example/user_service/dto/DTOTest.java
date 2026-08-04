package com.example.user_service.dto;

import com.example.user_service.commondto.UserDTO;
import com.example.user_service.dto.request.LearnerRequestDTO;
import com.example.user_service.dto.request.TrainerRequestDTO;
import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.dto.response.UserIdResponseDTO;
import com.example.user_service.dto.update.TrainerUpdateRequestDTO;
import com.example.user_service.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DTOTest {

    // ── UserDTO ──────────────────────────────────────────────────────────

    @Test
    void userDTO_settersAndGetters() {
        UserDTO dto = new UserDTO();
        dto.setUserId("u1");
        dto.setName("John");
        dto.setEmail("john@example.com");
        dto.setUsername("john123");
        dto.setIsActive(true);
        dto.setIsVerified(true);
        dto.setLastLogin(1000L);
        dto.setRole(Role.LEARNER);

        assertEquals("u1", dto.getUserId());
        assertEquals("John", dto.getName());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("john123", dto.getUsername());
        assertTrue(dto.getIsActive());
        assertTrue(dto.getIsVerified());
        assertEquals(1000L, dto.getLastLogin());
        assertEquals(Role.LEARNER, dto.getRole());
    }

    @Test
    void userDTO_builder() {
        UserDTO dto = UserDTO.builder()
                .userId("u2")
                .name("Jane")
                .email("jane@example.com")
                .build();
        assertEquals("u2", dto.getUserId());
        assertEquals("Jane", dto.getName());
    }

    // ── LearnerRequestDTO ────────────────────────────────────────────────

    @Test
    void learnerRequestDTO_settersAndGetters() {
        LearnerRequestDTO dto = new LearnerRequestDTO();
        dto.setUserId("l1");
        dto.setUsername("learner1");
        dto.setEmail("learner@example.com");
        dto.setFullName("Learner One");
        dto.setPhoneNumber("9876543210");
        dto.setDateOfBirth(LocalDate.of(1995, Month.MAY, 10));
        dto.setGender("Male");
        dto.setLinkedinUrl("https://linkedin.com");
        dto.setGithubUrl("https://github.com");
        dto.setAccountStatus(true);

        assertEquals("l1", dto.getUserId());
        assertEquals("learner1", dto.getUsername());
        assertEquals("learner@example.com", dto.getEmail());
        assertEquals("Learner One", dto.getFullName());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals(LocalDate.of(1995, Month.MAY, 10), dto.getDateOfBirth());
        assertEquals("Male", dto.getGender());
        assertEquals("https://linkedin.com", dto.getLinkedinUrl());
        assertEquals("https://github.com", dto.getGithubUrl());
        assertTrue(dto.isAccountStatus());
    }

    @Test
    void learnerRequestDTO_builder() {
        LearnerRequestDTO dto = LearnerRequestDTO.builder()
                .userId("l2")
                .email("l2@example.com")
                .build();
        assertEquals("l2", dto.getUserId());
    }

    // ── LearnerResponseDTO ───────────────────────────────────────────────

    @Test
    void learnerResponseDTO_settersAndGetters() {
        LearnerResponseDTO dto = new LearnerResponseDTO();
        dto.setUsername("user1");
        dto.setEmail("user@example.com");
        dto.setFullName("User One");
        dto.setPhoneNumber("9876543210");
        dto.setDateOfBirth(LocalDate.of(1995, Month.MAY, 10));
        dto.setGender("Female");
        dto.setLinkedinUrl("https://linkedin.com");
        dto.setGithubUrl("https://github.com");
        dto.setEmailVerified(true);
        dto.setAccountStatus(true);
        LocalDateTime now = LocalDateTime.of(2026, Month.JULY, 27, 10, 0);
        dto.setUpdatedAt(now);

        assertEquals("user1", dto.getUsername());
        assertEquals("user@example.com", dto.getEmail());
        assertEquals("User One", dto.getFullName());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals(LocalDate.of(1995, Month.MAY, 10), dto.getDateOfBirth());
        assertEquals("Female", dto.getGender());
        assertEquals("https://linkedin.com", dto.getLinkedinUrl());
        assertEquals("https://github.com", dto.getGithubUrl());
        assertTrue(dto.getEmailVerified());
        assertTrue(dto.getAccountStatus());
        assertEquals(now, dto.getUpdatedAt());
    }

    // ── TrainerRequestDTO ────────────────────────────────────────────────

    @Test
    void trainerRequestDTO_settersAndGetters() {
        TrainerRequestDTO dto = new TrainerRequestDTO();
        dto.setFullName("Trainer One");
        dto.setDesignation("Senior");
        dto.setPhoneNumber("9876543210");
        dto.setEmail("trainer@example.com");
        dto.setGender("Male");
        dto.setOfficeName("TechCorp");
        dto.setOfficeAddress("Hyderabad");
        dto.setLinkedInOrWebsiteURL("https://linkedin.com");
        dto.setGithubUrl("https://github.com");
        dto.setLanguageKnown(List.of("English"));
        dto.setTrainingSpecialization("Java");
        dto.setYearsOfExperience(5);
        dto.setQualification("B.Tech");
        dto.setDateOfBirth(LocalDate.of(1990, Month.JANUARY, 1));
        dto.setModesOfTrainingPreferred("Online");
        dto.setClientsTrainedBefore("Company A");
        dto.setPanNumber("ABCDE1234F");
        dto.setBankName("HDFC");
        dto.setBranchName("Main");
        dto.setAccountNumber("1234567890");
        dto.setIfscCode("HDFC0001234");
        dto.setGlobalCertifications("AWS");
        dto.setTopRegistration("Top");
        dto.setSupportingDocumentsChecklist("Doc1");
        dto.setAnyLegalDisputesInPast5Years(false);

        assertEquals("Trainer One", dto.getFullName());
        assertEquals("Senior", dto.getDesignation());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals("trainer@example.com", dto.getEmail());
        assertEquals("Male", dto.getGender());
        assertEquals("TechCorp", dto.getOfficeName());
        assertEquals("Hyderabad", dto.getOfficeAddress());
        assertEquals("https://linkedin.com", dto.getLinkedInOrWebsiteURL());
        assertEquals("https://github.com", dto.getGithubUrl());
        assertEquals(List.of("English"), dto.getLanguageKnown());
        assertEquals("Java", dto.getTrainingSpecialization());
        assertEquals(5, dto.getYearsOfExperience());
        assertEquals("B.Tech", dto.getQualification());
        assertEquals(LocalDate.of(1990, Month.JANUARY, 1), dto.getDateOfBirth());
        assertEquals("Online", dto.getModesOfTrainingPreferred());
        assertEquals("Company A", dto.getClientsTrainedBefore());
        assertEquals("ABCDE1234F", dto.getPanNumber());
        assertEquals("HDFC", dto.getBankName());
        assertEquals("Main", dto.getBranchName());
        assertEquals("1234567890", dto.getAccountNumber());
        assertEquals("HDFC0001234", dto.getIfscCode());
        assertEquals("AWS", dto.getGlobalCertifications());
        assertEquals("Top", dto.getTopRegistration());
        assertEquals("Doc1", dto.getSupportingDocumentsChecklist());
        assertFalse(dto.getAnyLegalDisputesInPast5Years());
    }

    // ── TrainerUpdateRequestDTO ──────────────────────────────────────────

    @Test
    void trainerUpdateRequestDTO_settersAndGetters() {
        TrainerUpdateRequestDTO dto = new TrainerUpdateRequestDTO();
        dto.setDesignation("Lead");
        dto.setPhoneNumber("9123456780");
        dto.setOfficeName("NewCorp");
        dto.setOfficeAddress("Mumbai");
        dto.setLinkedInOrWebsiteURL("https://linkedin.com");
        dto.setTrainingSpecialization("Spring Boot");
        dto.setYearsOfExperience(7);
        dto.setQualification("M.Tech");
        dto.setModesOfTrainingPreferred("Offline");
        dto.setClientsTrainedBefore("Company B");
        dto.setProfileImageURL("new.jpg");
        dto.setPanNumber("XYZAB1234C");
        dto.setBankName("SBI");
        dto.setBranchName("Branch");
        dto.setAccountNumber("0987654321");
        dto.setIfscCode("SBIN0001234");
        dto.setGlobalCertifications("GCP");
        dto.setTopRegistration("Yes");
        dto.setSupportingDocumentsChecklist("Doc2");
        dto.setAnyLegalDisputesInPast5Years(true);

        assertEquals("Lead", dto.getDesignation());
        assertEquals("9123456780", dto.getPhoneNumber());
        assertEquals("NewCorp", dto.getOfficeName());
        assertEquals("Mumbai", dto.getOfficeAddress());
        assertEquals("https://linkedin.com", dto.getLinkedInOrWebsiteURL());
        assertEquals("Spring Boot", dto.getTrainingSpecialization());
        assertEquals(7, dto.getYearsOfExperience());
        assertEquals("M.Tech", dto.getQualification());
        assertEquals("Offline", dto.getModesOfTrainingPreferred());
        assertEquals("Company B", dto.getClientsTrainedBefore());
        assertEquals("new.jpg", dto.getProfileImageURL());
        assertEquals("XYZAB1234C", dto.getPanNumber());
        assertEquals("SBI", dto.getBankName());
        assertEquals("Branch", dto.getBranchName());
        assertEquals("0987654321", dto.getAccountNumber());
        assertEquals("SBIN0001234", dto.getIfscCode());
        assertEquals("GCP", dto.getGlobalCertifications());
        assertEquals("Yes", dto.getTopRegistration());
        assertEquals("Doc2", dto.getSupportingDocumentsChecklist());
        assertTrue(dto.getAnyLegalDisputesInPast5Years());
    }

    // ── TrainerResponseDTO ───────────────────────────────────────────────

    @Test
    void trainerResponseDTO_coreFields() {
        TrainerResponseDTO dto = new TrainerResponseDTO();
        dto.setUserId("t1");
        dto.setFullName("Trainer");
        dto.setDesignation("Senior");
        dto.setPhoneNumber("9876543210");
        dto.setEmailId("trainer@example.com");
        dto.setLanguageKnown(List.of("English"));
        dto.setOfficeName("TechCorp");
        dto.setContentUrl("https://s3/demo.mp4");
        dto.setResumeUrl("https://s3/resume.pdf");
        dto.setRating(4.5f);
        dto.setOfficeAddress("Hyderabad");
        dto.setLinkedInOrWebsiteURL("https://linkedin.com");
        dto.setGithubURL("https://github.com");
        dto.setTrainingSpecialization("Java");
        dto.setYearsOfExperience(5);
        dto.setQualification("B.Tech");
        dto.setModesOfTrainingPreferred("Online");
        dto.setClientsTrainedBefore("Company A");
        dto.setProfilePictureURL("profile.jpg");
        dto.setDateOfBirth(LocalDate.of(1990, Month.JANUARY, 1));
        LocalDateTime now = LocalDateTime.of(2026, Month.JULY, 27, 10, 0);
        dto.setUpdatedAt(now);
        dto.setCreatedAt(now);

        assertEquals("t1", dto.getUserId());
        assertEquals("Trainer", dto.getFullName());
        assertEquals("Senior", dto.getDesignation());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals("trainer@example.com", dto.getEmailId());
        assertEquals(List.of("English"), dto.getLanguageKnown());
        assertEquals("TechCorp", dto.getOfficeName());
        assertEquals("https://s3/demo.mp4", dto.getContentUrl());
        assertEquals("https://s3/resume.pdf", dto.getResumeUrl());
        assertEquals(4.5f, dto.getRating());
        assertEquals("Hyderabad", dto.getOfficeAddress());
        assertEquals("https://linkedin.com", dto.getLinkedInOrWebsiteURL());
        assertEquals("https://github.com", dto.getGithubURL());
        assertEquals("Java", dto.getTrainingSpecialization());
        assertEquals(5, dto.getYearsOfExperience());
        assertEquals("B.Tech", dto.getQualification());
        assertEquals("Online", dto.getModesOfTrainingPreferred());
        assertEquals("Company A", dto.getClientsTrainedBefore());
        assertEquals("profile.jpg", dto.getProfilePictureURL());
        assertEquals(LocalDate.of(1990, Month.JANUARY, 1), dto.getDateOfBirth());
        assertEquals(now, dto.getUpdatedAt());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void trainerResponseDTO_additionalFields() {
        TrainerResponseDTO dto = new TrainerResponseDTO();
        dto.setGender("Male");
        dto.setGlobalCertifications("AWS");
        dto.setTopRegistration("Top");
        dto.setAccountStatus(true);
        dto.setSupportingDocumentsChecklist("Doc1");
        dto.setAnyLegalDisputesInPast5Years(false);

        assertEquals("Male", dto.getGender());
        assertEquals("AWS", dto.getGlobalCertifications());
        assertEquals("Top", dto.getTopRegistration());
        assertTrue(dto.isAccountStatus());
        assertEquals("Doc1", dto.getSupportingDocumentsChecklist());
        assertFalse(dto.getAnyLegalDisputesInPast5Years());
    }

    // ── UserIdResponseDTO ────────────────────────────────────────────────

    @Test
    void userIdResponseDTO_settersAndGetters() {
        UserIdResponseDTO dto = new UserIdResponseDTO();
        dto.setUserId("u1");
        assertEquals("u1", dto.getUserId());
    }
}
