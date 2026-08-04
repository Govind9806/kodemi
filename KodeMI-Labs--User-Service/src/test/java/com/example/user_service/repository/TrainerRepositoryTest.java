package com.example.user_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.example.user_service.model.Trainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class TrainerRepositoryTest {

    @Mock
    private DynamoDBMapper dynamoDBMapper;

    @InjectMocks
    private TrainerRepository trainerRepository;

    private Trainer trainer;

    @BeforeEach
    void setUp() {
        trainer = Trainer.builder()
                .userId("T001")
                .designation("Senior Trainer")
                .phoneNumber("9876543210")
                .email("trainer@example.com")
                .officeName("FitPro Academy")
                .officeAddress("Mumbai")
                .linkedInOrWebsiteURL("https://linkedin.com/in/trainer")
                .trainingSpecialization("Strength Training")
                .yearsOfExperience(10)
                .qualification("MSc Sports Science")
                .modesOfTrainingPreferred("Online/Offline")
                .clientsTrainedBefore("Corporate Clients")
                .profileImageURL("https://image.com/profile.jpg")
                .panNumber("ABCDE1234F")
                .bankName("HDFC Bank")
                .branchName("Andheri")
                .accountNumber("1234567890")
                .ifscCode("HDFC0001234")
                .globalCertifications("ACE, NASM")
                .topRegistration("Yes")
                .supportingDocumentsChecklist("Submitted")
                .anyLegalDisputesInPast5Years(false)
                .build();
    }

    @Test
    void testSave() {
        // DynamoDBMapper.save() returns void, so we just verify it was called
        trainerRepository.save(trainer);

        verify(dynamoDBMapper, times(1)).save(trainer);
    }

    @Test
    void testFindById() {
        when(dynamoDBMapper.load(Trainer.class, "T001")).thenReturn(trainer);

        Trainer found = trainerRepository.findById("T001");

        assertNotNull(found);
        assertEquals("Strength Training", found.getTrainingSpecialization());
        verify(dynamoDBMapper).load(Trainer.class, "T001");
    }

    @Test
    void testFindAll_callsScan() {
        @SuppressWarnings("unchecked")
        com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList<Trainer> scanList = 
                mock(com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList.class);
        when(dynamoDBMapper.scan(eq(Trainer.class), any(DynamoDBScanExpression.class)))
                .thenReturn(scanList);

        List<Trainer> trainers = trainerRepository.findAll();

        assertNotNull(trainers);
        verify(dynamoDBMapper, times(1)).scan(eq(Trainer.class), any(DynamoDBScanExpression.class));
    }


    @Test
    void testDelete_WhenExists() {
        when(dynamoDBMapper.load(Trainer.class, "T001")).thenReturn(trainer);

        trainerRepository.delete("T001");

        verify(dynamoDBMapper).delete(trainer);
    }

    @Test
    void testDelete_WhenNotExists() {
        when(dynamoDBMapper.load(Trainer.class, "T002")).thenReturn(null);

        trainerRepository.delete("T002");

        verify(dynamoDBMapper, never()).delete(any());
    }
}