package com.example.user_service.dto.request;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnerRequestDTOTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        LearnerRequestDTO dto = new LearnerRequestDTO();

        dto.setUserId("user123");
        dto.setUsername("john_doe");
        dto.setEmail("john@example.com");
        dto.setFullName("John Doe");
        dto.setPhoneNumber("9876543210");
        dto.setDateOfBirth(LocalDate.of(2000, Month.JANUARY, 1));
        dto.setGender("Male");
        dto.setLinkedinUrl("linkedin.com/john");
        dto.setGithubUrl("github.com/john");
        dto.setAccountStatus(true);

        assertEquals("user123", dto.getUserId());
        assertEquals("john_doe", dto.getUsername());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals(LocalDate.of(2000, Month.JANUARY, 1), dto.getDateOfBirth());
        assertEquals("Male", dto.getGender());
        assertEquals("linkedin.com/john", dto.getLinkedinUrl());
        assertEquals("github.com/john", dto.getGithubUrl());
        assertTrue(dto.isAccountStatus());
    }



    @Test
    void testBuilder() {
        LocalDate dob = LocalDate.of(2001, Month.OCTOBER, 10);

        LearnerRequestDTO dto = LearnerRequestDTO.builder()
                .userId("user123")
                .username("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .phoneNumber("9876543210")
                .dateOfBirth(dob)
                .gender("Male")
                .linkedinUrl("linkedin.com/john")
                .githubUrl("github.com/john")
                .accountStatus(true)
                .build();

        assertEquals("user123", dto.getUserId());
        assertEquals("john_doe", dto.getUsername());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals(dob, dto.getDateOfBirth());
        assertEquals("Male", dto.getGender());
        assertEquals("linkedin.com/john", dto.getLinkedinUrl());
        assertEquals("github.com/john", dto.getGithubUrl());
        assertTrue(dto.isAccountStatus());
    }
}