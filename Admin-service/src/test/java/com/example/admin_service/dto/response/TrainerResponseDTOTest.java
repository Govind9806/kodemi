package com.example.admin_service.dto.response;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class TrainerResponseDTOTest {

    @Test
    void testTrainerResponseDTO() {
        LocalDate dob = LocalDate.of(1985, 1, 1);
        LocalDateTime now = LocalDateTime.now();

        TrainerResponseDTO dto = TrainerResponseDTO.builder()
                .userId("t1")
                .fullName("Trainer One")
                .designation("Senior Trainer")
                .phoneNumber("1231231234")
                .emailId("trainer@email.com")
                .officeName("HQ")
                .contentUrl("http://url")
                .rating("5.0")
                .officeAddress("Address")
                .linkedInURL("http://ln")
                .githubURL("http://gh")
                .trainingSpecialization("Java")
                .yearsOfExperience(10)
                .qualification("MS")
                .modesOfTrainingPreferred("Online")
                .clientsTrainedBefore("Many")
                .profilePictureURL("http://pic")
                .dateOfBirth(dob)
                .updatedAt(now)
                .createdAt(now)
                .gender("F")
                .globalCertifications("AWS")
                .topRegistration("Top")
                .accountStatus(true)
                .supportingDocumentsChecklist("Check")
                .anyLegalDisputesInPast5Years(false)
                .build();

        assertEquals("t1", dto.getUserId());
        assertEquals("Trainer One", dto.getFullName());
        assertTrue(dto.isAccountStatus());
        assertFalse(dto.getAnyLegalDisputesInPast5Years());

        TrainerResponseDTO emptyDto = new TrainerResponseDTO();
        emptyDto.setUserId("t2");
        assertEquals("t2", emptyDto.getUserId());
    }
}
