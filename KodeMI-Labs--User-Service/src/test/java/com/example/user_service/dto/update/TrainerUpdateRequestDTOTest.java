package com.example.user_service.dto.update;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerUpdateRequestDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidDTO() {
        TrainerUpdateRequestDTO dto = TrainerUpdateRequestDTO.builder()
                .designation("Trainer")
                .phoneNumber("9876543210")
                .yearsOfExperience(5)
                .build();

        Set<ConstraintViolation<TrainerUpdateRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidPhoneNumber() {
        TrainerUpdateRequestDTO dto = TrainerUpdateRequestDTO.builder()
                .phoneNumber("12345") // invalid
                .build();

        Set<ConstraintViolation<TrainerUpdateRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertEquals("Phone number must be 10 digits",
                violations.iterator().next().getMessage());
    }

    @Test
    void testNegativeYearsOfExperience() {
        TrainerUpdateRequestDTO dto = TrainerUpdateRequestDTO.builder()
                .yearsOfExperience(-1) // invalid
                .build();

        Set<ConstraintViolation<TrainerUpdateRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertEquals("Years of experience cannot be negative",
                violations.iterator().next().getMessage());
    }

    @Test
    void testGettersAndSetters() {
        TrainerUpdateRequestDTO dto = new TrainerUpdateRequestDTO();

        dto.setDesignation("Senior Trainer");
        dto.setPhoneNumber("9876543210");
        dto.setYearsOfExperience(10);

        assertEquals("Senior Trainer", dto.getDesignation());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals(10, dto.getYearsOfExperience());
    }

    @Test
    void testBuilder() {
        TrainerUpdateRequestDTO dto = TrainerUpdateRequestDTO.builder()
                .designation("Trainer")
                .phoneNumber("9876543210")
                .yearsOfExperience(3)
                .build();

        assertEquals("Trainer", dto.getDesignation());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals(3, dto.getYearsOfExperience());
    }
}