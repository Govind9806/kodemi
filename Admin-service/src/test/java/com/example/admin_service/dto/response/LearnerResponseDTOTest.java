package com.example.admin_service.dto.response;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class LearnerResponseDTOTest {

    @Test
    void testLearnerResponseDTO() {
        LocalDate dob = LocalDate.of(1990, 1, 1);
        LocalDateTime now = LocalDateTime.now();

        LearnerResponseDTO dto = LearnerResponseDTO.builder()
                .username("learner1")
                .email("learner@email.com")
                .fullName("Learner One")
                .phoneNumber("1234567890")
                .profilePictureUrl("http://url")
                .dateOfBirth(dob)
                .gender("M")
                .linkedinUrl("http://ln")
                .githubUrl("http://gh")
                .emailVerified(true)
                .accountStatus(true)
                .updatedAt(now)
                .build();

        assertEquals("learner1", dto.getUsername());
        assertTrue(dto.getEmailVerified());

        LearnerResponseDTO emptyDto = new LearnerResponseDTO();
        emptyDto.setUsername("learner2");
        assertEquals("learner2", emptyDto.getUsername());
    }
}
