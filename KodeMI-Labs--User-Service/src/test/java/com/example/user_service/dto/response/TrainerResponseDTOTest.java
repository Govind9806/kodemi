package com.example.user_service.dto.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerResponseDTOTest {


    @Test
    void testNoArgsConstructorAndSetters() {
        TrainerResponseDTO dto = new TrainerResponseDTO();

        dto.setUserId("user123");
        dto.setFullName("John Doe");
        dto.setDesignation("Senior Trainer");
        dto.setPhoneNumber("9876543210");
        dto.setEmailId("john@example.com");
        dto.setLanguageKnown(Arrays.asList("English", "Spanish"));
        dto.setOfficeName("Tech Academy");
        dto.setContentUrl("http://example.com/content");
        dto.setResumeUrl("http://example.com/resume");
        dto.setRating(4.5F);
        dto.setOfficeAddress("123 Street, City");
        dto.setLinkedInOrWebsiteURL("http://linkedin.com/johndoe");
        dto.setGithubURL("http://github.com/johndoe");
        dto.setTrainingSpecialization("Java, Spring Boot");
        dto.setYearsOfExperience(10);
        dto.setQualification("M.Sc Computer Science");
        dto.setModesOfTrainingPreferred("Online, Offline");
        dto.setClientsTrainedBefore("Company A, Company B");
        dto.setProfilePictureURL("http://example.com/profile.jpg");
        dto.setDateOfBirth(LocalDate.of(1990, Month.JANUARY, 1));
        dto.setUpdatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        dto.setCreatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        dto.setGender("Male");
        dto.setGlobalCertifications("Oracle, AWS");
        dto.setTopRegistration("Top Trainer 2023");
        dto.setAccountStatus(true);
        dto.setSupportingDocumentsChecklist("Doc1, Doc2");
        dto.setAnyLegalDisputesInPast5Years(false);

        assertEquals("user123", dto.getUserId());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("Senior Trainer", dto.getDesignation());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals("john@example.com", dto.getEmailId());
        assertTrue(dto.getLanguageKnown().contains("English"));
        assertTrue(dto.isAccountStatus());
        assertFalse(dto.getAnyLegalDisputesInPast5Years());
    }



    @Test
    void testBuilderPattern() {
        TrainerResponseDTO dto = TrainerResponseDTO.builder()
                .userId("user456")
                .fullName("Jane Smith")
                .accountStatus(false)
                .anyLegalDisputesInPast5Years(true)
                .build();

        assertEquals("user456", dto.getUserId());
        assertEquals("Jane Smith", dto.getFullName());
        assertFalse(dto.isAccountStatus());
        assertTrue(dto.getAnyLegalDisputesInPast5Years());
    }
}