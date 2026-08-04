package com.example.user_service.service;

import com.example.user_service.dto.response.TrainerAdminResponse;
import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.dto.update.TrainerUpdateRequestDTO;
import com.example.user_service.commondto.UserDTO;
import com.example.user_service.exception.TrainerNotFoundException;
import com.example.user_service.exception.UserException;
import com.example.user_service.exception.UserServiceException;
import com.example.user_service.feign.AuthClient;
import com.example.user_service.model.Trainer;
import com.example.user_service.repository.TrainerRepository;
import com.example.user_service.service.impl.FileServiceImpl;
import com.example.user_service.service.impl.TrainerServiceImpl;
import com.example.user_service.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class TrainerServiceImplExtendedTest {

    private TrainerRepository repository;
    private AuthClient authClient;
    private FileServiceImpl fileService;
    private TrainerServiceImpl trainerService;

    private static final String USER_ID = "usr_001";
    private static final String TEST_KEY = "1234567890abcdef"; // valid 16-byte AES key

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        repository = mock(TrainerRepository.class);
        authClient = mock(AuthClient.class);
        fileService = mock(FileServiceImpl.class);
        com.example.user_service.notification.NotificationPublisher notificationPublisher = mock(com.example.user_service.notification.NotificationPublisher.class);
        java.time.Instant fixedInstant = java.time.Instant.parse("2026-07-27T10:00:00Z");
        java.time.Clock fixedClock = java.time.Clock.fixed(fixedInstant, java.time.ZoneOffset.UTC);
        trainerService = new TrainerServiceImpl(repository, authClient, fileService, notificationPublisher, fixedClock);

        // Initialize EncryptionUtil with a known valid key
        EncryptionUtil.initStatic(TEST_KEY);
    }

    private Trainer buildTrainer() {
        Trainer trainer = new Trainer();
        trainer.setUserId(USER_ID);
        trainer.setFullName("Rajesh Kumar");
        trainer.setEmail("rajesh@example.com");
        trainer.setDemoContentKey("usr_001/demo.mp4");
        trainer.setResumeKey("usr_001/resume.pdf");
        trainer.setProfileImageURL("usr_001/profile.jpg");
        trainer.setCreatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        trainer.setUpdatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        trainer.setDateOfBirth(LocalDate.of(1990, Month.JANUARY, 1));
        // Use pre-encrypted placeholder values — real decrypt is tested separately
        trainer.setPanNumber(EncryptionUtil.encrypt("ABCDE1234F"));
        trainer.setAccountNumber(EncryptionUtil.encrypt("1234567890"));
        trainer.setIfscCode(EncryptionUtil.encrypt("HDFC0001234"));
        trainer.setBankName("HDFC");
        trainer.setBranchName("Hyderabad");
        return trainer;
    }

    private Trainer buildFallbackTrainer() {
        Trainer trainer = new Trainer();
        trainer.setUserId("userId");
        trainer.setEmail("email@test.com");
        trainer.setFullName("John");
        trainer.setCreatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        trainer.setUpdatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        return trainer;
    }

    // ── getAllTrainers error path ──────────────────────────────────────────

    @Test
    void getAllTrainers_repositoryThrows_throwsUserServiceException() {
        when(repository.findAll()).thenThrow(new RuntimeException("DB error"));
        assertThrows(UserServiceException.class, () -> trainerService.getAllTrainers());
    }

    // ── getAllTrainersForAdmin error path ─────────────────────────────────

    @Test
    void getAllTrainersForAdmin_repositoryThrows_throwsUserServiceException() {
        when(repository.findAll()).thenThrow(new RuntimeException("DB error"));
        assertThrows(UserServiceException.class, () -> trainerService.getAllTrainersForAdmin());
    }

    // ── getAllTrainersForAdmin with full trainer data ──────────────────────

    @Test
    void getAllTrainersForAdmin_mapsDecryptedFinancialFields() {
        Trainer trainer = buildTrainer();
        when(repository.findAll()).thenReturn(List.of(trainer));
        when(fileService.generatePresignedUrl(any())).thenReturn("https://s3/url");

        var result = trainerService.getAllTrainersForAdmin();

        assertEquals(1, result.size());
        assertEquals("ABCDE1234F", result.get(0).getPanNumber());
        assertEquals("1234567890", result.get(0).getAccountNumber());
        assertEquals("HDFC0001234", result.get(0).getIfscCode());
        assertEquals("HDFC", result.get(0).getBankName());
        assertEquals("Hyderabad", result.get(0).getBranchName());
    }

    // ── getAllTrainersForAdmin with no resume/profile ──────────────────────

    @Test
    void getAllTrainersForAdmin_noResumeOrProfile_skipsUrls() {
        Trainer trainer = buildTrainer();
        trainer.setResumeKey(null);
        trainer.setProfileImageURL(null);
        when(repository.findAll()).thenReturn(List.of(trainer));
        when(fileService.generatePresignedUrl(trainer.getDemoContentKey())).thenReturn("https://s3/demo");

        var result = trainerService.getAllTrainersForAdmin();

        assertEquals(1, result.size());
        assertNull(result.get(0).getResumeKey());
        assertNull(result.get(0).getProfileImageURL());
    }

    // ── updateTrainerProfile — all update fields ──────────────────────────

    @Test
    void updateTrainerProfile_updatesAllFields() {
        Trainer trainer = buildTrainer();
        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(repository.findById(USER_ID)).thenReturn(trainer);
        when(fileService.uploadFile(USER_ID, file)).thenReturn("usr_001/new.jpg");

        try (var mockedStatic = mockStatic(EncryptionUtil.class)) {
            mockedStatic.when(() -> EncryptionUtil.encrypt(any())).thenReturn("encValue");

            TrainerUpdateRequestDTO req = new TrainerUpdateRequestDTO();
            req.setDesignation("Lead Trainer");
            req.setPhoneNumber("9123456780");
            req.setOfficeName("NewCorp");
            req.setOfficeAddress("Mumbai");
            req.setLinkedInOrWebsiteURL("https://linkedin.com/new");
            req.setTrainingSpecialization("Spring Boot");
            req.setYearsOfExperience(7);
            req.setQualification("M.Tech");
            req.setModesOfTrainingPreferred("Offline");
            req.setClientsTrainedBefore("Company B");
            req.setPanNumber("XYZAB1234C");
            req.setBankName("SBI");
            req.setBranchName("Branch");
            req.setAccountNumber("0987654321");
            req.setIfscCode("SBIN0001234");
            req.setGlobalCertifications("GCP");
            req.setTopRegistration("Yes");
            req.setSupportingDocumentsChecklist("Doc2");
            req.setAnyLegalDisputesInPast5Years(true);

            String result = trainerService.updateTrainerProfile(USER_ID, req, file);

            assertEquals("Trainer Updated Successfully", result);
            assertEquals("Lead Trainer", trainer.getDesignation());
            assertEquals("9123456780", trainer.getPhoneNumber());
            assertEquals("NewCorp", trainer.getOfficeName());
            assertEquals("Mumbai", trainer.getOfficeAddress());
            assertEquals("https://linkedin.com/new", trainer.getLinkedInOrWebsiteURL());
            assertEquals("Spring Boot", trainer.getTrainingSpecialization());
            assertEquals(7, trainer.getYearsOfExperience());
            assertEquals("M.Tech", trainer.getQualification());
            assertEquals("Offline", trainer.getModesOfTrainingPreferred());
            assertEquals("Company B", trainer.getClientsTrainedBefore());
            assertEquals("SBI", trainer.getBankName());
            assertEquals("Branch", trainer.getBranchName());
            assertTrue(trainer.getAnyLegalDisputesInPast5Years());
        }
    }

    // ── updateTrainerProfile — null/empty fields are skipped ──────────────

    @Test
    void updateTrainerProfile_nullFields_notUpdated() {
        Trainer trainer = buildTrainer();
        trainer.setDesignation("Original");
        trainer.setPhoneNumber("9876543210");
        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(repository.findById(USER_ID)).thenReturn(trainer);
        when(fileService.uploadFile(USER_ID, file)).thenReturn("usr_001/new.jpg");

        try (var mockedStatic = mockStatic(EncryptionUtil.class)) {
            mockedStatic.when(() -> EncryptionUtil.encrypt(any())).thenReturn("encValue");

            TrainerUpdateRequestDTO req = new TrainerUpdateRequestDTO();
            // all fields null — nothing should change

            trainerService.updateTrainerProfile(USER_ID, req, file);

            assertEquals("Original", trainer.getDesignation());
            assertEquals("9876543210", trainer.getPhoneNumber());
        }
    }

    // ── deleteTrainerProfile error path ───────────────────────────────────

    @Test
    void deleteTrainerProfile_repositoryThrows_throwsUserServiceException() {
        doThrow(new RuntimeException("DB error")).when(repository).delete(USER_ID);
        assertThrows(UserServiceException.class, () -> trainerService.deleteTrainerProfile(USER_ID));
    }

    @Test
    void getTrainerProfileById_fallbackFails_throwsException() {
        when(repository.findById("nonexistent")).thenReturn(null);
        when(authClient.getUsersByIds(any())).thenThrow(new RuntimeException("fallback down"));
        assertThrows(TrainerNotFoundException.class, () -> trainerService.getTrainerProfileById("nonexistent"));
    }

    @Test
    void getTrainerProfileById_fallbackFound_returnsBasicProfile() {
        when(repository.findById("userId")).thenReturn(null).thenAnswer(invocation -> buildFallbackTrainer());
        UserDTO user = new UserDTO();
        user.setUserId("userId");
        user.setEmail("email@test.com");
        user.setName("John");
        when(authClient.getUsersByIds(List.of("userId"))).thenReturn(List.of(user));
        TrainerResponseDTO res = trainerService.getTrainerProfileById("userId");
        assertNotNull(res);
        assertEquals("email@test.com", res.getEmailId());
    }

    @Test
    void updateTrainerProfile_trainerNotFound_throwsTrainerNotFoundException() {
        when(repository.findById("userId")).thenReturn(null);
        TrainerUpdateRequestDTO updateDto = new TrainerUpdateRequestDTO();
        assertThrows(TrainerNotFoundException.class, () -> trainerService.updateTrainerProfile("userId", updateDto, null));
    }

    @Test
    void updateTrainerProfile_emptyUserId_throwsIllegalArgumentException() {
        TrainerUpdateRequestDTO updateDto = new TrainerUpdateRequestDTO();
        assertThrows(IllegalArgumentException.class, () -> trainerService.updateTrainerProfile("   ", updateDto, null));
    }

    @Test
    void updateTrainerProfile_exceptionThrown_throwsUserServiceException() {
        Trainer trainer = buildTrainer();
        when(repository.findById("userId")).thenReturn(trainer);
        doThrow(new RuntimeException("db down")).when(repository).save(any());
        TrainerUpdateRequestDTO updateDto = new TrainerUpdateRequestDTO();
        assertThrows(UserServiceException.class, () -> trainerService.updateTrainerProfile("userId", updateDto, null));
    }

    @Test
    void deleteTrainerProfile_emptyUserId_throwsUserException() {
        assertThrows(UserException.class, () -> trainerService.deleteTrainerProfile("   "));
    }

    @Test
    void getAllTrainers_emptyOrNull_returnsEmptyList() {
        when(repository.findAll()).thenReturn(null);
        List<TrainerResponseDTO> res = trainerService.getAllTrainers();
        assertTrue(res.isEmpty());
    }

    @Test
    void getAllTrainers_statusMapping_approvals() {
        Trainer t1 = buildTrainer(); t1.setUserId("u1");
        Trainer t2 = buildTrainer(); t2.setUserId("u2");
        Trainer t3 = buildTrainer(); t3.setUserId("u3");
        Trainer t4 = buildTrainer(); t4.setUserId("u4");

        when(repository.findAll()).thenReturn(List.of(t1, t2, t3, t4));
        UserDTO u1 = new UserDTO(); u1.setUserId("u1"); u1.setStatus("APPROVAL_PENDING");
        UserDTO u2 = new UserDTO(); u2.setUserId("u2"); u2.setStatus("ACTIVE");
        UserDTO u3 = new UserDTO(); u3.setUserId("u3"); u3.setStatus("REJECTED");
        UserDTO u4 = new UserDTO(); u4.setUserId("u4"); u4.setStatus("UNKNOWN");
        when(authClient.getUsersByIds(any())).thenReturn(List.of(u1, u2, u3, u4));

        List<TrainerResponseDTO> res = trainerService.getAllTrainers();
        assertEquals(4, res.size());
        assertEquals("PENDING", res.get(0).getStatus());
        assertEquals("APPROVED", res.get(1).getStatus());
        assertEquals("REJECTED", res.get(2).getStatus());
        assertEquals("PENDING", res.get(3).getStatus());
    }

    @Test
    void getAllTrainersForUsers_exceptionThrown_throwsUserServiceException() {
        when(repository.findAll()).thenThrow(new RuntimeException("db down"));
        assertThrows(UserServiceException.class, () -> trainerService.getAllTrainersForUsers());
    }

    @Test
    void getAllTrainersForAdmin_emptyOrNull_returnsEmptyList() {
        when(repository.findAll()).thenReturn(null);
        List<TrainerAdminResponse> res = trainerService.getAllTrainersForAdmin();
        assertTrue(res.isEmpty());
    }

    @Test
    void getAllTrainersForAdmin_exception_throwsUserServiceException() {
        when(repository.findAll()).thenThrow(new RuntimeException("db error"));
        assertThrows(UserServiceException.class, () -> trainerService.getAllTrainersForAdmin());
    }

    @Test
    void updateTrainerProfile_updateAllFields_success() {
        Trainer trainer = buildTrainer();
        when(repository.findById("userId")).thenReturn(trainer);

        TrainerUpdateRequestDTO dto = new TrainerUpdateRequestDTO();
        dto.setDesignation(" Senior Consultant ");
        dto.setPhoneNumber(" 9998887770 ");
        dto.setOfficeName(" HQ Office ");
        dto.setOfficeAddress(" 123 Main St ");
        dto.setLinkedInOrWebsiteURL(" https://linkedin.com/in/test ");
        dto.setTrainingSpecialization(" Cloud Architecture ");
        dto.setYearsOfExperience(12);
        dto.setQualification(" Ph.D ");
        dto.setModesOfTrainingPreferred("ONLINE");
        dto.setClientsTrainedBefore("Google");
        dto.setPanNumber(" ABCDE1234F ");
        dto.setBankName(" State Bank ");
        dto.setBranchName(" Main Branch ");
        dto.setAccountNumber(" 1122334455 ");
        dto.setIfscCode(" SBIN0001234 ");
        dto.setGlobalCertifications(" AWS Certified ");
        dto.setTopRegistration(" REG123 ");
        dto.setSupportingDocumentsChecklist("PAN");
        dto.setAnyLegalDisputesInPast5Years(false);

        MockMultipartFile resume = new MockMultipartFile("resume", "cv.pdf", "application/pdf", "resume".getBytes());

        when(fileService.uploadFile(org.mockito.ArgumentMatchers.eq("userId"), any())).thenReturn("resumes/cv.pdf");

        String res = trainerService.updateTrainerProfile("userId", dto, resume);

        assertNotNull(res);
        assertEquals("Senior Consultant", trainer.getDesignation());
        assertEquals("9998887770", trainer.getPhoneNumber());
        assertEquals("HQ Office", trainer.getOfficeName());
        assertEquals("123 Main St", trainer.getOfficeAddress());
        assertEquals("https://linkedin.com/in/test", trainer.getLinkedInOrWebsiteURL());
        assertEquals("Cloud Architecture", trainer.getTrainingSpecialization());
        assertEquals(12, trainer.getYearsOfExperience());
        assertEquals("Ph.D", trainer.getQualification());
    }
}