package com.example.user_service.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    Clock fixedClock = Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"),
            ZoneOffset.UTC
    );

    // ── Helper ───────────────────────────────────────────────────────────

    private TrainerRequestDTO buildValid() {
        return TrainerRequestDTO.builder()
                .fullName("Rajesh Kumar")
                .designation("Senior Trainer")
                .phoneNumber("9876543210")
                .email("rajesh@example.com")
                .gender("Male")
                .officeName("TechCorp")
                .officeAddress("Hyderabad")
                .languageKnown(List.of("English", "Telugu"))
                .trainingSpecialization("Java")
                .yearsOfExperience(5)
                .qualification("B.Tech")
                .dateOfBirth(LocalDate.of(1990, Month.JANUARY, 1))
                .panNumber("ABCDE1234F")
                .bankName("HDFC")
                .branchName("Hyderabad")
                .accountNumber("1234567890")
                .ifscCode("SBIN0001234")
                .languageKnown(java.util.List.of("English"))
                .build();
    }

    private Set<ConstraintViolation<TrainerRequestDTO>> validate(TrainerRequestDTO dto) {
        return validator.validate(dto);
    }

    private boolean hasViolationOnField(
            Set<ConstraintViolation<TrainerRequestDTO>> violations, String field) {
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    private String messageFor(
            Set<ConstraintViolation<TrainerRequestDTO>> violations, String field) {
        return violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(null);
    }

    // ── Valid DTO ────────────────────────────────────────────────────────

    @Test
    void validDTO_noViolations() {
        assertTrue(validate(buildValid()).isEmpty());
    }

    // ── fullName ─────────────────────────────────────────────────────────

    @Test
    void fullName_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setFullName("");
        assertTrue(hasViolationOnField(validate(dto), "fullName"));
    }

    @Test
    void fullName_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setFullName(null);
        assertTrue(hasViolationOnField(validate(dto), "fullName"));
    }

    // ── designation ──────────────────────────────────────────────────────

    @Test
    void designation_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setDesignation("");
        assertTrue(hasViolationOnField(validate(dto), "designation"));
    }

    @Test
    void designation_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setDesignation(null);
        assertTrue(hasViolationOnField(validate(dto), "designation"));
    }

    // ── phoneNumber ──────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "9876543210, false",
        "98765,      true",
        "98765432101, true",
        "98765abc10, true",
        "'',         true"
    })
    void phoneNumber_parameterized(String phoneNumber, boolean expectViolation) {
        TrainerRequestDTO dto = buildValid();
        dto.setPhoneNumber(phoneNumber);
        if (expectViolation) {
            assertTrue(hasViolationOnField(validate(dto), "phoneNumber"));
        } else {
            assertTrue(validate(dto).isEmpty());
        }
    }

    @Test
    void phoneNumber_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setPhoneNumber(null);
        assertTrue(hasViolationOnField(validate(dto), "phoneNumber"));
    }

    // ── email ────────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "test@domain.com,  false",
        "notanemail,       true",
        "rajeshexample.com, true",
        "'',               true"
    })
    void email_parameterized(String email, boolean expectViolation) {
        TrainerRequestDTO dto = buildValid();
        dto.setEmail(email);
        if (expectViolation) {
            assertTrue(hasViolationOnField(validate(dto), "email"));
        } else {
            assertTrue(validate(dto).isEmpty());
        }
    }

    @Test
    void email_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setEmail(null);
        assertTrue(hasViolationOnField(validate(dto), "email"));
    }

    // ── gender ───────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "Male",
        "Female",
        "Other"
    })
    void gender_valid_noViolation(String gender) {
        TrainerRequestDTO dto = buildValid();
        dto.setGender(gender);
        assertTrue(validate(dto).isEmpty());
    }

    @Test
    void gender_invalid_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setGender("Unknown");
        assertTrue(hasViolationOnField(validate(dto), "gender"));
        assertEquals("Gender must be Male, Female, or Other",
                messageFor(validate(dto), "gender"));
    }

    @Test
    void gender_lowercase_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setGender("male");   // case-sensitive pattern
        assertTrue(hasViolationOnField(validate(dto), "gender"));
    }

    @Test
    void gender_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setGender(null);
        assertTrue(hasViolationOnField(validate(dto), "gender"));
    }

    // ── officeName ───────────────────────────────────────────────────────

    @Test
    void officeName_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setOfficeName("");
        assertTrue(hasViolationOnField(validate(dto), "officeName"));
    }

    @Test
    void officeName_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setOfficeName(null);
        assertTrue(hasViolationOnField(validate(dto), "officeName"));
    }

    // ── officeAddress ────────────────────────────────────────────────────

    @Test
    void officeAddress_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setOfficeAddress("");
        assertTrue(hasViolationOnField(validate(dto), "officeAddress"));
    }

    @Test
    void officeAddress_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setOfficeAddress(null);
        assertTrue(hasViolationOnField(validate(dto), "officeAddress"));
    }

    // ── linkedInOrWebsiteURL / githubUrl — optional ───────────────────────

    @Test
    void linkedInOrWebsiteURL_null_noViolation() {
        TrainerRequestDTO dto = buildValid();
        dto.setLinkedInOrWebsiteURL(null);
        assertTrue(validate(dto).isEmpty());
    }

    @Test
    void githubUrl_null_noViolation() {
        TrainerRequestDTO dto = buildValid();
        dto.setGithubUrl(null);
        assertTrue(validate(dto).isEmpty());
    }

    // ── languageKnown ────────────────────────────────────────────────────

    @Test
    void languageKnown_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setLanguageKnown(null);
        assertTrue(hasViolationOnField(validate(dto), "languageKnown"));
    }

    @Test
    void languageKnown_emptyList_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setLanguageKnown(List.of());
        assertTrue(hasViolationOnField(validate(dto), "languageKnown"));
        assertEquals("Trainer Languages known is required",
                messageFor(validate(dto), "languageKnown"));
    }

    @Test
    void languageKnown_multipleValues_noViolation() {
        TrainerRequestDTO dto = buildValid();
        dto.setLanguageKnown(List.of("English", "Hindi", "Telugu"));
        assertTrue(validate(dto).isEmpty());
    }

    // ── trainingSpecialization ───────────────────────────────────────────

    @Test
    void trainingSpecialization_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setTrainingSpecialization("");
        assertTrue(hasViolationOnField(validate(dto), "trainingSpecialization"));
    }

    @Test
    void trainingSpecialization_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setTrainingSpecialization(null);
        assertTrue(hasViolationOnField(validate(dto), "trainingSpecialization"));
    }

    // ── yearsOfExperience ────────────────────────────────────────────────

    @Test
    void yearsOfExperience_zero_noViolation() {
        TrainerRequestDTO dto = buildValid();
        dto.setYearsOfExperience(0);
        assertTrue(validate(dto).isEmpty());
    }

    @Test
    void yearsOfExperience_positive_noViolation() {
        TrainerRequestDTO dto = buildValid();
        dto.setYearsOfExperience(10);
        assertTrue(validate(dto).isEmpty());
    }

    @Test
    void yearsOfExperience_negative_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setYearsOfExperience(-1);
        assertTrue(hasViolationOnField(validate(dto), "yearsOfExperience"));
        assertEquals("Years of experience cannot be negative",
                messageFor(validate(dto), "yearsOfExperience"));
    }

    @Test
    void yearsOfExperience_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setYearsOfExperience(null);
        assertTrue(hasViolationOnField(validate(dto), "yearsOfExperience"));
    }

    // ── qualification ────────────────────────────────────────────────────

    @Test
    void qualification_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setQualification("");
        assertTrue(hasViolationOnField(validate(dto), "qualification"));
    }

    @Test
    void qualification_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setQualification(null);
        assertTrue(hasViolationOnField(validate(dto), "qualification"));
    }

    // ── dateOfBirth ──────────────────────────────────────────────────────

    @Test
    void dateOfBirth_past_noViolation() {
        TrainerRequestDTO dto = buildValid();
        dto.setDateOfBirth(LocalDate.of(1995, Month.JUNE, 15));
        assertTrue(validate(dto).isEmpty());
    }

    @Test
    void dateOfBirth_today_noViolation() {
        TrainerRequestDTO dto = buildValid();
        dto.setDateOfBirth(LocalDate.of(2000, Month.JANUARY, 1));
        assertTrue(validate(dto).isEmpty());
    }

    @Test
    void dateOfBirth_future_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setDateOfBirth(LocalDate.of(2099, Month.DECEMBER, 31));
        assertTrue(hasViolationOnField(validate(dto), "dateOfBirth"));
        assertEquals("Date of birth cannot be in the future",
                messageFor(validate(dto), "dateOfBirth"));
    }

    @Test
    void dateOfBirth_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setDateOfBirth(null);
        assertTrue(hasViolationOnField(validate(dto), "dateOfBirth"));
    }

    // ── optional fields — modesOfTrainingPreferred, clientsTrainedBefore ─

    @Test
    void optionalFields_null_noViolation() {
        TrainerRequestDTO dto = buildValid();
        dto.setModesOfTrainingPreferred(null);
        dto.setClientsTrainedBefore(null);
        dto.setGlobalCertifications(null);
        dto.setTopRegistration(null);
        dto.setSupportingDocumentsChecklist(null);
        dto.setAnyLegalDisputesInPast5Years(null);
        assertTrue(validate(dto).isEmpty());
    }

    // ── financial fields ─────────────────────────────────────────────────

    @Test
    void panNumber_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setPanNumber("");
        assertTrue(hasViolationOnField(validate(dto), "panNumber"));
    }

    @Test
    void panNumber_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setPanNumber(null);
        assertTrue(hasViolationOnField(validate(dto), "panNumber"));
    }

    @Test
    void bankName_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setBankName("");
        assertTrue(hasViolationOnField(validate(dto), "bankName"));
    }

    @Test
    void branchName_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setBranchName("");
        assertTrue(hasViolationOnField(validate(dto), "branchName"));
    }

    @Test
    void accountNumber_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setAccountNumber("");
        assertTrue(hasViolationOnField(validate(dto), "accountNumber"));
    }

    @Test
    void accountNumber_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setAccountNumber(null);
        assertTrue(hasViolationOnField(validate(dto), "accountNumber"));
    }

    @Test
    void ifscCode_blank_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setIfscCode("");
        assertTrue(hasViolationOnField(validate(dto), "ifscCode"));
    }

    @Test
    void ifscCode_null_violation() {
        TrainerRequestDTO dto = buildValid();
        dto.setIfscCode(null);
        assertTrue(hasViolationOnField(validate(dto), "ifscCode"));
    }

    // ── multiple violations ───────────────────────────────────────────────

    @Test
    void multipleFields_invalid_multipleViolations() {
        TrainerRequestDTO dto = buildValid();
        dto.setFullName(null);
        dto.setEmail("bademail");
        dto.setPhoneNumber("123");
        dto.setGender("alien");
        dto.setYearsOfExperience(-5);
        dto.setDateOfBirth(LocalDate.of(2099, Month.DECEMBER, 31));

        Set<ConstraintViolation<TrainerRequestDTO>> violations = validate(dto);

        assertTrue(violations.size() >= 6);
        assertTrue(hasViolationOnField(violations, "fullName"));
        assertTrue(hasViolationOnField(violations, "email"));
        assertTrue(hasViolationOnField(violations, "phoneNumber"));
        assertTrue(hasViolationOnField(violations, "gender"));
        assertTrue(hasViolationOnField(violations, "yearsOfExperience"));
        assertTrue(hasViolationOnField(violations, "dateOfBirth"));
    }
}