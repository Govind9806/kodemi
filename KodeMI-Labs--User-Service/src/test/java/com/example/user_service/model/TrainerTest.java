package com.example.user_service.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        Trainer trainer = new Trainer();

        trainer.setUserId("user123");
        trainer.setFullName("John Doe");
        trainer.setGender("Male");
        trainer.setDesignation("Senior Trainer");
        trainer.setPhoneNumber("1234567890");
        trainer.setEmail("john.doe@example.com");
        trainer.setRatingValue(4.5F);
        trainer.setLanguageKnown(List.of("English", "French"));
        trainer.setDemoContentKey("demoKey");
        trainer.setResumeKey("resumeKey");
        trainer.setOfficeName("Tech Academy");
        trainer.setOfficeAddress("123 Street, City");
        trainer.setLinkedInOrWebsiteURL("http://linkedin.com/johndoe");
        trainer.setGitHubUrl("http://github.com/johndoe");
        trainer.setTrainingSpecialization("Java, Spring Boot");
        trainer.setYearsOfExperience(10);
        trainer.setQualification("M.Sc Computer Science");
        trainer.setModesOfTrainingPreferred("Online, Offline");
        trainer.setClientsTrainedBefore("Company A, Company B");
        trainer.setProfileImageURL("http://example.com/profile.jpg");
        trainer.setCreatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        trainer.setUpdatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        trainer.setPanNumber("ABCDE1234F");
        trainer.setDateOfBirth(LocalDate.of(1990, Month.JANUARY, 1));
        trainer.setBankName("Bank A");
        trainer.setBranchName("Branch X");
        trainer.setAccountNumber("1234567890");
        trainer.setIfscCode("IFSC0001");
        trainer.setGlobalCertifications("Oracle, AWS");
        trainer.setTopRegistration("Top Trainer 2023");
        trainer.setSupportingDocumentsChecklist("Doc1, Doc2");
        trainer.setAnyLegalDisputesInPast5Years(false);

        assertThat(trainer.getUserId()).isEqualTo("user123");
        assertThat(trainer.getFullName()).isEqualTo("John Doe");
        assertThat(trainer.getLinkedInOrWebsiteURL()).isEqualTo("http://linkedin.com/johndoe");
        assertThat(trainer.getLanguageKnown()).containsExactly("English", "French");
        assertThat(trainer.getYearsOfExperience()).isEqualTo(10);
        assertThat(trainer.getDateOfBirth()).isEqualTo(LocalDate.of(1990, Month.JANUARY, 1));
        assertThat(trainer.getAnyLegalDisputesInPast5Years()).isFalse();
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        LocalDate dob = LocalDate.of(1990, Month.JANUARY, 1);
        LocalDateTime now = LocalDateTime.of(2026, Month.JULY, 27, 10, 0);

        Trainer trainer = new Trainer(
                "user123",
                "John Doe",
                "Male",
                "Senior Trainer",
                "1234567890",
                "john.doe@example.com",
                4.5F,
                List.of("English", "French"),
                "demoKey",
                "resumeKey",
                "Tech Academy",
                "123 Street, City",
                "http://linkedin.com/johndoe",
                "http://github.com/johndoe",
                "Java, Spring Boot",
                10,
                "M.Sc Computer Science",
                "Online, Offline",
                "Company A, Company B",
                "http://example.com/profile.jpg",
                now,
                now,
                "ABCDE1234F",
                dob,
                "Bank A",
                "Branch X",
                "1234567890",
                "IFSC0001",
                "Oracle, AWS",
                "Top Trainer 2023",
                "Doc1, Doc2",
                false
        );

        assertThat(trainer.getUserId()).isEqualTo("user123");
        assertThat(trainer.getFullName()).isEqualTo("John Doe");
        assertThat(trainer.getGender()).isEqualTo("Male");
        assertThat(trainer.getYearsOfExperience()).isEqualTo(10);
        assertThat(trainer.getDateOfBirth()).isEqualTo(dob);
        assertThat(trainer.getAnyLegalDisputesInPast5Years()).isFalse();
    }

    @Test
    void testBuilder() {
        LocalDate dob = LocalDate.of(1990, Month.JANUARY, 1);
        LocalDateTime now = LocalDateTime.of(2026, Month.JULY, 27, 10, 0);

        Trainer trainer = Trainer.builder()
                .userId("user123")
                .fullName("John Doe")
                .gender("Male")
                .designation("Senior Trainer")
                .phoneNumber("1234567890")
                .email("john.doe@example.com")
                .ratingValue(4.5F)
                .demoContentKey("demo123")
                .officeName("Main Office")
                .officeAddress("123 Street")
                .linkedInOrWebsiteURL("https://linkedin.com/johndoe")
                .gitHubUrl("https://github.com/johndoe")
                .trainingSpecialization("Java")
                .yearsOfExperience(10)
                .dateOfBirth(dob)
                .createdAt(now)
                .updatedAt(now)
                .anyLegalDisputesInPast5Years(false)
                .build();

        assertThat(trainer.getUserId()).isEqualTo("user123");
        assertThat(trainer.getFullName()).isEqualTo("John Doe");
        assertThat(trainer.getGender()).isEqualTo("Male");
        assertThat(trainer.getYearsOfExperience()).isEqualTo(10);
        assertThat(trainer.getDateOfBirth()).isEqualTo(dob);
        assertThat(trainer.getAnyLegalDisputesInPast5Years()).isFalse();
    }
}