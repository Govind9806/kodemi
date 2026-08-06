package com.example.user_service.service;

import com.example.user_service.dto.request.TrainerRequestDTO;
import com.example.user_service.dto.response.TrainerAdminResponse;
import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.dto.response.TrainerUserResponseDTO;
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
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class TrainerServiceImplTest {

    private TrainerRepository  repository;
    private AuthClient         authClient;
    private FileServiceImpl    fileService;
    private com.example.user_service.notification.NotificationPublisher notificationPublisher;
    private TrainerServiceImpl trainerService;

    private static final String USER_ID = "usr_001";
    private static final String TOKEN   = "Bearer dummyToken";
    private static final String DEMO_KEY    = "usr_001/demo.mp4";
    private static final String RESUME_KEY  = "usr_001/resume.pdf";
    private static final String PROFILE_KEY = "usr_001/profile.jpg";

    @BeforeEach
    void setup() {
        repository     = mock(TrainerRepository.class);
        authClient     = mock(AuthClient.class);
        fileService    = mock(FileServiceImpl.class);
        notificationPublisher = mock(com.example.user_service.notification.NotificationPublisher.class);
        java.time.Instant fixedInstant = java.time.Instant.parse("2026-07-27T10:00:00Z");
        java.time.Clock fixedClock = java.time.Clock.fixed(fixedInstant, java.time.ZoneOffset.UTC);
        trainerService = new TrainerServiceImpl(repository, authClient, fileService, notificationPublisher, fixedClock);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private TrainerRequestDTO buildRequest() {
        TrainerRequestDTO dto = new TrainerRequestDTO();
        dto.setFullName("Rajesh Kumar");
        dto.setDesignation("Senior Trainer");
        dto.setPhoneNumber("9876543210");
        dto.setEmail("rajesh@example.com");
        dto.setGender("Male");
        dto.setTrainingSpecialization("Java");
        dto.setYearsOfExperience(5);
        dto.setQualification("B.Tech");
        dto.setPanNumber("ABCDE1234F");
        dto.setAccountNumber("1234567890");
        dto.setIfscCode("HDFC0001234");
        dto.setBankName("HDFC");
        dto.setBranchName("Hyderabad");
        return dto;
    }

    private Trainer buildTrainer() {
        Trainer trainer = new Trainer();
        trainer.setUserId(USER_ID);
        trainer.setFullName("Rajesh Kumar");
        trainer.setEmail("rajesh@example.com");
        trainer.setDemoContentKey(DEMO_KEY);
        trainer.setResumeKey(RESUME_KEY);
        trainer.setProfileImageURL(PROFILE_KEY);
        trainer.setCreatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        trainer.setUpdatedAt(LocalDateTime.of(2026, Month.JULY, 27, 10, 0));
        trainer.setDateOfBirth(LocalDate.of(1990, Month.JANUARY, 1));
        trainer.setPanNumber("encPan");
        trainer.setAccountNumber("encAcc");
        trainer.setIfscCode("encIfsc");
        trainer.setBankName("HDFC");
        trainer.setBranchName("Hyderabad");
        return trainer;
    }

    private MockMultipartFile buildFile() {
        return new MockMultipartFile(
                "demo", "demo.mp4", "video/mp4", new byte[]{1, 2, 3});
    }

    private MockMultipartFile buildEmptyFile() {
        return new MockMultipartFile("file", new byte[0]);
    }

    private void mockPresignedUrls() {
        when(fileService.generatePresignedUrl(DEMO_KEY)).thenReturn("https://s3/demo.mp4");
        when(fileService.generatePresignedUrl(RESUME_KEY)).thenReturn("https://s3/resume.pdf");
        when(fileService.generatePresignedUrl(PROFILE_KEY)).thenReturn("https://s3/profile.jpg");
    }

    // ── createTrainerProfile ─────────────────────────────────────────────

    @Test
    void createTrainerProfile_success_returnsSuccessMessage() {
        MockMultipartFile demo    = buildFile();
        MockMultipartFile resume  = buildFile();
        MockMultipartFile profile = buildFile();

        when(repository.findById(USER_ID)).thenReturn(null);
        when(fileService.uploadFile(USER_ID, demo)).thenReturn(DEMO_KEY);
        when(fileService.uploadFile(USER_ID, resume)).thenReturn(RESUME_KEY);
        when(fileService.uploadFile(USER_ID, profile)).thenReturn(PROFILE_KEY);

        com.example.user_service.dto.request.TrainerProfileFiles files = new com.example.user_service.dto.request.TrainerProfileFiles(
                demo, profile, null, null, resume);

        String result = trainerService.createTrainerProfile(
                buildRequest(), files, USER_ID, TOKEN);

        assertEquals("Trainer Registered Successfully.", result);
        verify(repository, times(1)).save(any(Trainer.class));
        verify(authClient, times(1)).updateUserRole(USER_ID, "TRAINER", TOKEN);
        verify(authClient, times(1)).markTrainerSubmitted(USER_ID, TOKEN);
    }

    @Test
    void createTrainerProfile_emptyProfile_skipsProfileUpload() {
        MockMultipartFile demo    = buildFile();
        MockMultipartFile resume  = buildFile();
        MockMultipartFile profile = buildEmptyFile();   // empty — should be skipped

        when(repository.findById(USER_ID)).thenReturn(null);
        when(fileService.uploadFile(USER_ID, demo)).thenReturn(DEMO_KEY);
        when(fileService.uploadFile(USER_ID, resume)).thenReturn(RESUME_KEY);

        com.example.user_service.dto.request.TrainerProfileFiles files = new com.example.user_service.dto.request.TrainerProfileFiles(
                demo, resume, null, null, profile);

        String result = trainerService.createTrainerProfile(
                buildRequest(), files, USER_ID, TOKEN);

        assertEquals("Trainer Registered Successfully.", result);
        // profile upload must NOT be called since file is empty
        verify(fileService, never()).uploadFile(USER_ID, profile);
    }

    @Test
    void createTrainerProfile_alreadyExists_returnsAlreadyRegistered() {
        when(repository.findById(USER_ID)).thenReturn(buildTrainer());

        com.example.user_service.dto.request.TrainerProfileFiles files = new com.example.user_service.dto.request.TrainerProfileFiles(
                buildFile(), null, null, null, null);

        String result = trainerService.createTrainerProfile(
                buildRequest(), files, USER_ID, TOKEN);
        assertEquals("Trainer Already Registered.", result);
        verify(repository, never()).save(any());
    }

    @Test
    void createTrainerProfile_nullRequest_throwsNullPointerException() {
        MockMultipartFile demo    = buildFile();
        MockMultipartFile resume  = buildFile();
        MockMultipartFile profile = buildFile();
        com.example.user_service.dto.request.TrainerProfileFiles files = new com.example.user_service.dto.request.TrainerProfileFiles(
                demo, profile, null, null, resume);
        assertThrows(NullPointerException.class, () ->
                trainerService.createTrainerProfile(null, files, USER_ID, TOKEN));
    }

    @Test
    void getTrainerProfileById_notFound() {
        when(repository.findById("user123")).thenReturn(null);
        when(authClient.getUsersByIds(Collections.singletonList("user123"))).thenReturn(Collections.emptyList());
        assertThrows(TrainerNotFoundException.class, () -> trainerService.getTrainerProfileById("user123"));
    }

    @Test
    void createTrainerProfile_emptyUserId_throwsUserException() {
        TrainerRequestDTO req     = buildRequest();
        MockMultipartFile demo    = buildFile();
        MockMultipartFile resume  = buildFile();
        MockMultipartFile profile = buildFile();
        com.example.user_service.dto.request.TrainerProfileFiles files = new com.example.user_service.dto.request.TrainerProfileFiles(
                demo, profile, null, null, resume);
        assertThrows(UserException.class, () ->
                trainerService.createTrainerProfile(req, files, "  ", TOKEN));
    }

    @Test
    void createTrainerProfile_authClientFails_throwsUserServiceException() {
        when(repository.findById(USER_ID)).thenReturn(null);
        when(fileService.uploadFile(any(), any())).thenReturn(DEMO_KEY);
        doThrow(new RuntimeException("Feign error"))
                .when(authClient).updateUserRole(USER_ID, "TRAINER", TOKEN);
        TrainerRequestDTO req     = buildRequest();
        MockMultipartFile demo    = buildFile();
        MockMultipartFile resume  = buildFile();
        MockMultipartFile profile = buildFile();
        com.example.user_service.dto.request.TrainerProfileFiles files = new com.example.user_service.dto.request.TrainerProfileFiles(
                demo, profile, null, null, resume);
        assertThrows(UserServiceException.class, () ->
                trainerService.createTrainerProfile(req, files, USER_ID, TOKEN));
    }

    // ── getTrainerProfileById ────────────────────────────────────────────

    @Test
    void getTrainerProfileById_success_returnsDTO() {
        when(repository.findById(USER_ID)).thenReturn(buildTrainer());
        mockPresignedUrls();

        TrainerResponseDTO result = trainerService.getTrainerProfileById(USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals("Rajesh Kumar", result.getFullName());
        assertEquals("https://s3/demo.mp4", result.getContentUrl());
        assertEquals("https://s3/resume.pdf", result.getResumeUrl());
        assertEquals("https://s3/profile.jpg", result.getProfilePictureURL());
    }

    @Test
    void updateTrainerProfile_withoutFile_success() {
        Trainer trainer = new Trainer();
        trainer.setProfileImageURL("existingKey");

        when(repository.findById("user123")).thenReturn(trainer);

        TrainerUpdateRequestDTO updateDTO = new TrainerUpdateRequestDTO();
        updateDTO.setDesignation("Lead Trainer");
        updateDTO.setOfficeName("New Office");

        String result = trainerService.updateTrainerProfile("user123", updateDTO, null);

        assertEquals("Trainer Updated Successfully", result);
        assertEquals("existingKey", trainer.getProfileImageURL()); // unchanged
        assertEquals("Lead Trainer", trainer.getDesignation());
        assertEquals("New Office", trainer.getOfficeName());
        verify(fileService, never()).uploadFile(anyString(), any());
        verify(fileService, never()).deleteFile(anyString());
        verify(repository, times(1)).save(trainer);
    }

    @Test
    void deleteTrainerProfile_success() {
        doNothing().when(repository).delete(USER_ID);
        String result = trainerService.deleteTrainerProfile(USER_ID);
        assertEquals("Trainer Deleted successfully", result);
        verify(repository, times(1)).delete(USER_ID);
    }

    @Test
    void deleteTrainerProfile_repositoryThrows_throwsUserServiceException() {
        doThrow(new RuntimeException("DB error")).when(repository).delete(USER_ID);

        assertThrows(UserServiceException.class, () ->
                trainerService.deleteTrainerProfile(USER_ID));
    }

    @Test
    void getAllPendingTrainers_success() {
        when(authClient.getPendingTrainers()).thenReturn(Collections.singletonList("user123"));
        Trainer trainer = new Trainer();
        trainer.setUserId("user123");
        trainer.setDemoContentKey("key123");
        when(repository.findByIds(Collections.singletonList("user123"))).thenReturn(Collections.singletonList(trainer));
        when(fileService.generatePresignedUrl("key123")).thenReturn("url");

        List<TrainerResponseDTO> result = trainerService.getAllPendingTrainers();
        assertEquals(1, result.size());
        assertEquals("user123", result.get(0).getUserId());
    }

    @Test
    void getAllTrainers_emptyList_returnsEmpty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<TrainerResponseDTO> result = trainerService.getAllTrainers();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllTrainers_repositoryThrows_throwsUserServiceException() {
        when(repository.findAll()).thenThrow(new RuntimeException("DB error"));

        assertThrows(UserServiceException.class, () -> trainerService.getAllTrainers());
    }

    // ── getAllTrainersForAdmin ─────────────────────────────────────────────

    @Test
    void getAllTrainersForAdmin_returnsMappedList() {
        Trainer trainer = buildTrainer();
        when(repository.findAll()).thenReturn(List.of(trainer));
        mockPresignedUrls();

        // EncryptionUtil is static — mock its decrypt by setting known values
        try (var mockedStatic = mockStatic(EncryptionUtil.class)) {
            mockedStatic.when(() -> EncryptionUtil.decrypt("encPan")).thenReturn("ABCDE1234F");
            mockedStatic.when(() -> EncryptionUtil.decrypt("encAcc")).thenReturn("1234567890");
            mockedStatic.when(() -> EncryptionUtil.decrypt("encIfsc")).thenReturn("HDFC0001234");

            List<TrainerAdminResponse> result = trainerService.getAllTrainersForAdmin();

            assertEquals(1, result.size());
            assertEquals(USER_ID, result.get(0).getUserId());
            assertEquals("ABCDE1234F", result.get(0).getPanNumber());
            assertEquals("1234567890", result.get(0).getAccountNumber());
        }
    }

    @Test
    void getAllTrainersForAdmin_emptyList_returnsEmpty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<TrainerAdminResponse> result = trainerService.getAllTrainersForAdmin();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllTrainersForAdmin_repositoryThrows_throwsUserServiceException() {
        when(repository.findAll()).thenThrow(new RuntimeException("DB error"));

        assertThrows(UserServiceException.class, () -> trainerService.getAllTrainersForAdmin());
    }

    // ── getAllPendingTrainers ──────────────────────────────────────────────

    @Test
    void getAllPendingTrainers_returnsMappedList() {
        when(authClient.getPendingTrainers()).thenReturn(List.of(USER_ID));
        when(repository.findByIds(List.of(USER_ID))).thenReturn(List.of(buildTrainer()));
        mockPresignedUrls();

        List<TrainerResponseDTO> result = trainerService.getAllPendingTrainers();

        assertEquals(1, result.size());
        assertEquals(USER_ID, result.get(0).getUserId());
    }

    @Test
    void getAllPendingTrainers_emptyList_returnsEmpty() {
        when(authClient.getPendingTrainers()).thenReturn(Collections.emptyList());

        List<TrainerResponseDTO> result = trainerService.getAllPendingTrainers();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllTrainersForUsers_success() {
        Trainer trainer1 = new Trainer();
        trainer1.setUserId("u1");
        trainer1.setDemoContentKey("key1");
        trainer1.setProfileImageURL("profile1");
        trainer1.setLinkedInOrWebsiteURL("linkedin1");
        trainer1.setGitHubUrl("github1");

        Trainer trainer2 = new Trainer();
        trainer2.setUserId("u2");
        trainer2.setDemoContentKey("key2");
        trainer2.setProfileImageURL("profile2");
        trainer2.setLinkedInOrWebsiteURL("linkedin2");
        trainer2.setGitHubUrl("github2");

        UserDTO user1 = new UserDTO();
        user1.setUserId("u1");
        user1.setStatus("ACTIVE");

        UserDTO user2 = new UserDTO();
        user2.setUserId("u2");
        user2.setStatus("PENDING");

        when(repository.findAll()).thenReturn(List.of(trainer1, trainer2));
        when(authClient.getUsersByIds(List.of("u1", "u2"))).thenReturn(List.of(user1, user2));
        when(fileService.generatePresignedUrl("key1")).thenReturn("url1");
        when(fileService.generatePresignedUrl("profile1")).thenReturn("prof1");

        List<TrainerUserResponseDTO> response = trainerService.getAllTrainersForUsers();
        assertEquals(1, response.size());
        TrainerUserResponseDTO dto = response.get(0);
        assertEquals("u1", dto.getUserId());
        assertEquals("linkedin1", dto.getLinkedInURL());
        assertEquals("github1", dto.getGithubURL());
        assertTrue(dto.getIsVerified());
    }
}