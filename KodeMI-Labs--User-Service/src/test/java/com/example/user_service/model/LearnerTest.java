package com.example.user_service.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnerTest {

    @Test
    void testBuilder() {
        LocalDate dob = LocalDate.of(1995, Month.MAY, 15);
        LocalDateTime now = LocalDateTime.of(2026, Month.JULY, 27, 10, 0);
        
        Learner learner = Learner.builder()
                .userId("learner123")
                .username("johndoe")
                .email("john@example.com")
                .fullName("John Doe")
                .phoneNumber("1234567890")
                .profilePictureKey("profile/pic.jpg")
                .dateOfBirth(dob)
                .gender("Male")
                .emailVerified(true)
                .phoneVerified(true)
                .accountStatus(true)
                .createdAt(now)
                .updatedAt(now)
                .linkedinUrl("https://linkedin.com/in/johndoe")
                .githubUrl("https://github.com/johndoe")
                .build();

        assertEquals("learner123", learner.getUserId());
        assertEquals("johndoe", learner.getUsername());
        assertEquals("john@example.com", learner.getEmail());
        assertEquals("John Doe", learner.getFullName());
        assertEquals("1234567890", learner.getPhoneNumber());
        assertEquals("profile/pic.jpg", learner.getProfilePictureKey());
        assertEquals(dob, learner.getDateOfBirth());
        assertEquals("Male", learner.getGender());
        assertTrue(learner.getEmailVerified());
        assertTrue(learner.getPhoneVerified());
        assertTrue(learner.isAccountStatus());
        assertEquals(now, learner.getCreatedAt());
        assertEquals(now, learner.getUpdatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        Learner learner = new Learner();
        assertNotNull(learner);
    }
}
