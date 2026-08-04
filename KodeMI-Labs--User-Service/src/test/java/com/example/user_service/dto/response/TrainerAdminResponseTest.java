package com.example.user_service.dto.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerAdminResponseTest {

    private TrainerAdminResponse buildSampleDTO() {
        TrainerAdminResponse dto = new TrainerAdminResponse();
        dto.setUserId("user123");
        dto.setFullName("John Doe");
        dto.setGender("Male");
        dto.setDesignation("Senior Trainer");
        dto.setPhoneNumber("9876543210");
        dto.setEmail("john@example.com");
        dto.setRatingValue(4.5F);
        dto.setLanguageKnown(Arrays.asList("English", "Spanish"));
        dto.setDemoContentKey("demoKey123");
        dto.setResumeKey("resumeKey123");
        dto.setOfficeName("Tech Academy");
        dto.setOfficeAddress("123 Street, City");
        dto.setLinkedInOrWebsiteURL("http://linkedin.com/johndoe");
        dto.setGitHubUrl("http://github.com/johndoe");
        dto.setTrainingSpecialization("Java, Spring Boot");
        dto.setYearsOfExperience(10);
        dto.setQualification("M.Sc Computer Science");
        dto.setModesOfTrainingPreferred("Online, Offline");
        dto.setClientsTrainedBefore("Company A, Company B");
        dto.setProfileImageURL("http://example.com/profile.jpg");
        dto.setCreatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        dto.setUpdatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        dto.setPanNumber("ABCDE1234F");
        dto.setDateOfBirth(LocalDate.of(1990, Month.JANUARY, 1));
        dto.setBankName("SBI");
        dto.setBranchName("Main");
        dto.setAccountNumber("1234567890");
        dto.setIfscCode("SBIN0001234");
        dto.setGlobalCertifications("Oracle, AWS");
        dto.setTopRegistration("Top Trainer 2023");
        dto.setSupportingDocumentsChecklist("Doc1, Doc2");
        dto.setAnyLegalDisputesInPast5Years(false);
        return dto;
    }

    @Test
    void testCoreIdentityAndContactFields() {
        TrainerAdminResponse dto = buildSampleDTO();

        assertEquals("user123", dto.getUserId());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("Male", dto.getGender());
        assertEquals("Senior Trainer", dto.getDesignation());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals(4.5F, dto.getRatingValue());
        assertTrue(dto.getLanguageKnown().contains("English"));
        assertEquals("demoKey123", dto.getDemoContentKey());
        assertEquals("resumeKey123", dto.getResumeKey());
        assertEquals("Tech Academy", dto.getOfficeName());
        assertEquals("123 Street, City", dto.getOfficeAddress());
        assertEquals("http://linkedin.com/johndoe", dto.getLinkedInOrWebsiteURL());
        assertEquals("http://github.com/johndoe", dto.getGitHubUrl());
        assertEquals("Java, Spring Boot", dto.getTrainingSpecialization());
    }

    @Test
    void testProfessionalAndFinancialFields() {
        TrainerAdminResponse dto = buildSampleDTO();

        assertEquals(10, dto.getYearsOfExperience());
        assertEquals("M.Sc Computer Science", dto.getQualification());
        assertEquals("Online, Offline", dto.getModesOfTrainingPreferred());
        assertEquals("Company A, Company B", dto.getClientsTrainedBefore());
        assertEquals("http://example.com/profile.jpg", dto.getProfileImageURL());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
        assertEquals("ABCDE1234F", dto.getPanNumber());
        assertEquals(LocalDate.of(1990, Month.JANUARY, 1), dto.getDateOfBirth());
        assertEquals("SBI", dto.getBankName());
        assertEquals("Main", dto.getBranchName());
        assertEquals("1234567890", dto.getAccountNumber());
        assertEquals("SBIN0001234", dto.getIfscCode());
        assertEquals("Oracle, AWS", dto.getGlobalCertifications());
        assertEquals("Top Trainer 2023", dto.getTopRegistration());
        assertEquals("Doc1, Doc2", dto.getSupportingDocumentsChecklist());
        assertFalse(dto.getAnyLegalDisputesInPast5Years());
    }
}