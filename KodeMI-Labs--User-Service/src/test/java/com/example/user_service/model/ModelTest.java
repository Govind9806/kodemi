package com.example.user_service.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {

    // ── Trainer ──────────────────────────────────────────────────────────

    @Test
    void trainer_coreFields() {
        Trainer t = new Trainer();
        t.setUserId("u1");
        t.setFullName("John");
        t.setGender("Male");
        t.setDesignation("Senior");
        t.setPhoneNumber("9876543210");
        t.setEmail("john@example.com");
        t.setRatingValue(4.5f);
        t.setLanguageKnown(List.of("English"));
        t.setDemoContentKey("demo.mp4");
        t.setResumeKey("resume.pdf");
        t.setOfficeName("TechCorp");
        t.setOfficeAddress("Hyderabad");
        t.setLinkedInOrWebsiteURL("https://linkedin.com");
        t.setGitHubUrl("https://github.com");
        t.setTrainingSpecialization("Java");
        t.setYearsOfExperience(5);
        t.setQualification("B.Tech");
        t.setModesOfTrainingPreferred("Online");
        t.setClientsTrainedBefore("Company A");
        t.setProfileImageURL("profile.jpg");
        LocalDateTime now = LocalDateTime.of(2026, Month.JULY, 27, 10, 0);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);

        assertEquals("u1", t.getUserId());
        assertEquals("John", t.getFullName());
        assertEquals("Male", t.getGender());
        assertEquals("Senior", t.getDesignation());
        assertEquals("9876543210", t.getPhoneNumber());
        assertEquals("john@example.com", t.getEmail());
        assertEquals(4.5f, t.getRatingValue());
        assertEquals(List.of("English"), t.getLanguageKnown());
        assertEquals("demo.mp4", t.getDemoContentKey());
        assertEquals("resume.pdf", t.getResumeKey());
        assertEquals("TechCorp", t.getOfficeName());
        assertEquals("Hyderabad", t.getOfficeAddress());
        assertEquals("https://linkedin.com", t.getLinkedInOrWebsiteURL());
        assertEquals("https://github.com", t.getGitHubUrl());
        assertEquals("Java", t.getTrainingSpecialization());
        assertEquals(5, t.getYearsOfExperience());
        assertEquals("B.Tech", t.getQualification());
        assertEquals("Online", t.getModesOfTrainingPreferred());
        assertEquals("Company A", t.getClientsTrainedBefore());
        assertEquals("profile.jpg", t.getProfileImageURL());
        assertEquals(now, t.getCreatedAt());
        assertEquals(now, t.getUpdatedAt());
    }

    @Test
    void trainer_financialAndLegalFields() {
        Trainer t = new Trainer();
        t.setPanNumber("ABCDE1234F");
        t.setDateOfBirth(LocalDate.of(1990, Month.JANUARY, 1));
        t.setBankName("HDFC");
        t.setBranchName("Main");
        t.setAccountNumber("1234567890");
        t.setIfscCode("HDFC0001234");
        t.setGlobalCertifications("AWS");
        t.setTopRegistration("Top");
        t.setSupportingDocumentsChecklist("Doc1");
        t.setAnyLegalDisputesInPast5Years(false);

        assertEquals("ABCDE1234F", t.getPanNumber());
        assertEquals(LocalDate.of(1990, Month.JANUARY, 1), t.getDateOfBirth());
        assertEquals("HDFC", t.getBankName());
        assertEquals("Main", t.getBranchName());
        assertEquals("1234567890", t.getAccountNumber());
        assertEquals("HDFC0001234", t.getIfscCode());
        assertEquals("AWS", t.getGlobalCertifications());
        assertEquals("Top", t.getTopRegistration());
        assertEquals("Doc1", t.getSupportingDocumentsChecklist());
        assertFalse(t.getAnyLegalDisputesInPast5Years());
    }

    @Test
    void trainer_builder() {
        Trainer t = Trainer.builder()
                .userId("u2")
                .fullName("Jane")
                .email("jane@example.com")
                .build();
        assertEquals("u2", t.getUserId());
        assertEquals("Jane", t.getFullName());
        assertEquals("jane@example.com", t.getEmail());
    }

    @Test
    void trainer_noArgsConstructor() {
        Trainer t = new Trainer();
        assertNull(t.getUserId());
    }

    // ── Learner ──────────────────────────────────────────────────────────

    @Test
    void learner_settersAndGetters() {
        Learner l = new Learner();
        l.setUserId("l1");
        l.setUsername("user1");
        l.setEmail("user@example.com");
        l.setFullName("User One");
        l.setPhoneNumber("9876543210");
        l.setProfilePictureKey("pic.jpg");
        l.setDateOfBirth(LocalDate.of(1995, Month.MAY, 10));
        l.setGender("Female");
        l.setEmailVerified(true);
        l.setPhoneVerified(false);
        l.setAccountStatus(true);
        LocalDateTime now = LocalDateTime.of(2026, Month.JULY, 27, 10, 0);
        l.setCreatedAt(now);
        l.setUpdatedAt(now);
        l.setLinkedinUrl("https://linkedin.com");
        l.setGithubUrl("https://github.com");

        assertEquals("l1", l.getUserId());
        assertEquals("user1", l.getUsername());
        assertEquals("user@example.com", l.getEmail());
        assertEquals("User One", l.getFullName());
        assertEquals("9876543210", l.getPhoneNumber());
        assertEquals("pic.jpg", l.getProfilePictureKey());
        assertEquals(LocalDate.of(1995, Month.MAY, 10), l.getDateOfBirth());
        assertEquals("Female", l.getGender());
        assertTrue(l.getEmailVerified());
        assertFalse(l.getPhoneVerified());
        assertTrue(l.isAccountStatus());
        assertEquals(now, l.getCreatedAt());
        assertEquals(now, l.getUpdatedAt());
        assertEquals("https://linkedin.com", l.getLinkedinUrl());
        assertEquals("https://github.com", l.getGithubUrl());
    }

    @Test
    void learner_builder() {
        Learner l = Learner.builder()
                .userId("l2")
                .email("l2@example.com")
                .build();
        assertEquals("l2", l.getUserId());
        assertEquals("l2@example.com", l.getEmail());
    }

    // ── Rating ───────────────────────────────────────────────────────────

    @Test
    void rating_settersAndGetters() {
        Rating r = new Rating();
        r.setUserId("u1");
        r.setTrainerId("t1");
        r.setRatingValue(4.0f);

        assertEquals("u1", r.getUserId());
        assertEquals("t1", r.getTrainerId());
        assertEquals(4.0f, r.getRatingValue());
    }

    @Test
    void rating_allArgsConstructor() {
        Rating r = new Rating("u1", "t1", 5.0f);
        assertEquals("u1", r.getUserId());
        assertEquals("t1", r.getTrainerId());
        assertEquals(5.0f, r.getRatingValue());
    }

    @Test
    void rating_builder() {
        Rating r = Rating.builder()
                .userId("u3")
                .trainerId("t3")
                .ratingValue(3.5f)
                .build();
        assertEquals("u3", r.getUserId());
        assertEquals("t3", r.getTrainerId());
        assertEquals(3.5f, r.getRatingValue());
    }

    // ── AccountStatus ────────────────────────────────────────────────────

    @Test
    void accountStatus_values() {
        AccountStatus[] values = AccountStatus.values();
        assertTrue(values.length > 0);
        assertNotNull(AccountStatus.valueOf(values[0].name()));
    }
}
