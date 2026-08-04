package com.example.user_service.dto.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnerResponseDTOTest {

    @Test
    void testBuilder() {
        LocalDate dob = LocalDate.of(1995, Month.MAY, 15);
        LocalDateTime updatedAt = LocalDateTime.of(2026, Month.JULY, 27, 10, 0);
        
        LearnerResponseDTO dto = LearnerResponseDTO.builder()
                .username("johndoe")
                .email("john@example.com")
                .fullName("John Doe")
                .phoneNumber("1234567890")
                .dateOfBirth(dob)
                .gender("Male")
                .linkedinUrl("https://linkedin.com/in/johndoe")
                .githubUrl("https://github.com/johndoe")
                .emailVerified(true)
                .accountStatus(true)
                .updatedAt(updatedAt)
                .build();

        assertEquals("johndoe", dto.getUsername());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("1234567890", dto.getPhoneNumber());
        assertEquals(dob, dto.getDateOfBirth());
        assertEquals("Male", dto.getGender());
        assertEquals("https://linkedin.com/in/johndoe", dto.getLinkedinUrl());
        assertEquals("https://github.com/johndoe", dto.getGithubUrl());
        assertTrue(dto.getEmailVerified());
        assertTrue(dto.getAccountStatus());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        LearnerResponseDTO dto = new LearnerResponseDTO();
        assertNotNull(dto);
    }

    @Test
    void testSettersAndGetters() {
        LearnerResponseDTO dto = new LearnerResponseDTO();
        LocalDate dob = LocalDate.of(1998, Month.MARCH, 20);
        LocalDateTime updatedAt = LocalDateTime.of(2026, Month.JULY, 27, 10, 0);
        
        dto.setUsername("janedoe");
        dto.setEmail("jane@example.com");
        dto.setFullName("Jane Doe");
        dto.setPhoneNumber("9876543210");
        dto.setDateOfBirth(dob);
        dto.setGender("Female");
        dto.setLinkedinUrl("https://linkedin.com/in/janedoe");
        dto.setGithubUrl("https://github.com/janedoe");
        dto.setEmailVerified(false);
        dto.setAccountStatus(false);
        dto.setUpdatedAt(updatedAt);

        assertEquals("janedoe", dto.getUsername());
        assertEquals("jane@example.com", dto.getEmail());
        assertEquals("Jane Doe", dto.getFullName());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals(dob, dto.getDateOfBirth());
        assertEquals("Female", dto.getGender());
        assertFalse(dto.getEmailVerified());
        assertFalse(dto.getAccountStatus());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    void testBooleanFields() {
        LearnerResponseDTO dto = new LearnerResponseDTO();
        
        dto.setEmailVerified(true);
        assertTrue(dto.getEmailVerified());
        
        dto.setEmailVerified(false);
        assertFalse(dto.getEmailVerified());
        
        dto.setAccountStatus(true);
        assertTrue(dto.getAccountStatus());
        
        dto.setAccountStatus(false);
        assertFalse(dto.getAccountStatus());
    }

    @Test
    void testNullValues() {
        LearnerResponseDTO dto = LearnerResponseDTO.builder().build();
        
        assertNull(dto.getUsername());
        assertNull(dto.getEmail());
        assertNull(dto.getFullName());
        assertNull(dto.getPhoneNumber());
        assertNull(dto.getDateOfBirth());
        assertNull(dto.getGender());
        assertNull(dto.getLinkedinUrl());
        assertNull(dto.getGithubUrl());
        assertNull(dto.getEmailVerified());
        assertNull(dto.getUpdatedAt());
    }
}
