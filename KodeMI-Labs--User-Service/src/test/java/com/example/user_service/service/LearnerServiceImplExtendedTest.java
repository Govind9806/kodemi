package com.example.user_service.service;

import com.example.user_service.commondto.UserDTO;
import com.example.user_service.dto.request.LearnerRequestDTO;
import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.exception.UserException;
import com.example.user_service.exception.UserServiceException;
import com.example.user_service.model.Learner;
import com.example.user_service.repository.LearnerRepository;
import com.example.user_service.service.impl.FileServiceImpl;
import com.example.user_service.service.impl.LearnerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Executable;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

class LearnerServiceImplExtendedTest {

    @Mock
    private LearnerRepository repository;

    @Mock
    private FileServiceImpl fileService;

    @Mock
    private com.example.user_service.repository.LearnerFollowRepository followRepository;

    @InjectMocks
    private LearnerServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        java.time.Instant fixedInstant = java.time.Instant.parse("2026-07-27T10:00:00Z");
        java.time.Clock fixedClock = java.time.Clock.fixed(fixedInstant, java.time.ZoneOffset.UTC);
        service.setClock(fixedClock);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "followRepository", followRepository);
    }

    // ── createLearner edge cases ──────────────────────────────────────────

    @Test
    void createLearner_nullUserId_throwsNullPointerException() {
        UserDTO dto = new UserDTO();
        dto.setUserId(null);
        assertThrows(NullPointerException.class, () -> service.createLearner(dto));
    }

    @Test
    void createLearner_repositoryThrows_throwsUserServiceException() {
        UserDTO dto = new UserDTO();
        dto.setUserId("u1");
        dto.setEmail("u1@example.com");
        doThrow(new RuntimeException("DB error")).when(repository).save(any());

        assertThrows(UserServiceException.class, () -> service.createLearner(dto));
    }

    @Test
    void createLearner_isVerifiedNull_setsEmailVerifiedFalse() {
        UserDTO dto = new UserDTO();
        dto.setUserId("u1");
        dto.setIsVerified(null);
        dto.setIsActive(null);

        service.createLearner(dto);

        verify(repository).save(argThat(l -> !l.getEmailVerified() && !l.isAccountStatus()));
    }

    // ── getProfileByUserId edge cases ─────────────────────────────────────

    @Test
    void getProfileByUserId_repositoryThrows_throwsUserServiceException() {
        when(repository.findByUserId("u1")).thenThrow(new RuntimeException("DB error"));

        assertThrows(UserServiceException.class, () -> service.getProfileByUserId("u1"));
    }

    @Test
    void getProfileByUserId_mapsAllFields() {
        Learner learner = new Learner();
        learner.setUserId("u1");
        learner.setUsername("user1");
        learner.setEmail("user@example.com");
        learner.setFullName("User One");
        learner.setPhoneNumber("9876543210");
        learner.setDateOfBirth(LocalDate.of(1995, Month.MAY, 10));
        learner.setGender("Male");
        learner.setLinkedinUrl("https://linkedin.com");
        learner.setGithubUrl("https://github.com");
        learner.setEmailVerified(true);
        learner.setAccountStatus(true);

        when(repository.findByUserId("u1")).thenReturn(learner);

        LearnerResponseDTO dto = service.getProfileByUserId("u1");

        assertEquals("user1", dto.getUsername());
        assertEquals("user@example.com", dto.getEmail());
        assertEquals("User One", dto.getFullName());
        assertEquals("9876543210", dto.getPhoneNumber());
        assertEquals(LocalDate.of(1995, Month.MAY, 10), dto.getDateOfBirth());
        assertEquals("Male", dto.getGender());
        assertEquals("https://linkedin.com", dto.getLinkedinUrl());
        assertEquals("https://github.com", dto.getGithubUrl());
        assertTrue(dto.getEmailVerified());
        assertTrue(dto.getAccountStatus());
    }

    // ── getAllProfiles edge cases ──────────────────────────────────────────

    @Test
    void getAllProfiles_emptyList_returnsEmpty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());
        List<LearnerResponseDTO> result = service.getAllProfiles();
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllProfiles_nullList_returnsEmpty() {
        when(repository.findAll()).thenReturn(null);
        List<LearnerResponseDTO> result = service.getAllProfiles();
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllProfiles_repositoryThrows_throwsUserServiceException() {
        when(repository.findAll()).thenThrow(new RuntimeException("DB error"));
        assertThrows(UserServiceException.class, () -> service.getAllProfiles());
    }

    // ── updateProfile edge cases ──────────────────────────────────────────

    @Test
    void updateProfile_nullUserId_throwsNullPointerException() {
        LearnerRequestDTO dto = new LearnerRequestDTO();
        assertThrows(NullPointerException.class,
                () -> service.updateProfile(null, dto, null));
    }

    @Test
    void updateProfile_emptyUserId_throwsUserException() {
        LearnerRequestDTO dto = new LearnerRequestDTO();
        assertThrows(UserException.class,
                () -> service.updateProfile("  ", dto, null));
    }

    @Test
    void updateProfile_nullDto_doesNotThrow() {
        Learner learner = new Learner();
        learner.setUserId("u1");
        when(repository.findByUserId("u1")).thenReturn(learner);

        assertDoesNotThrow(() -> service.updateProfile("u1", null, null));
    }

    @Test
    void updateProfile_repositoryThrows_throwsUserServiceException() {
        Learner learner = new Learner();
        learner.setUserId("u1");
        when(repository.findByUserId("u1")).thenReturn(learner);
        doThrow(new RuntimeException("DB error")).when(repository).save(any());

        LearnerRequestDTO dto = new LearnerRequestDTO();
        assertThrows(UserServiceException.class,
                () -> service.updateProfile("u1", dto, null));
    }

    @Test
    void updateProfile_withFile_noExistingKey_uploadsWithoutDelete() {
        Learner learner = new Learner();
        learner.setUserId("u1");
        learner.setProfilePictureKey(null); // no existing key
        when(repository.findByUserId("u1")).thenReturn(learner);

        MultipartFile file = mock(MultipartFile.class);
        when(fileService.uploadFile("u1", file)).thenReturn("newKey");

        LearnerRequestDTO dto = new LearnerRequestDTO();
        service.updateProfile("u1", dto, file);

        verify(fileService).uploadFile("u1", file);
        verify(fileService, never()).deleteFile(any());
        assertEquals("newKey", learner.getProfilePictureKey());
    }

    @Test
    void updateProfile_updatesAllFields() {
        Learner learner = new Learner();
        learner.setUserId("u1");
        when(repository.findByUserId("u1")).thenReturn(learner);

        LearnerRequestDTO dto = new LearnerRequestDTO();
        dto.setUsername("newUser");
        dto.setEmail("new@example.com");
        dto.setFullName("New Name");
        dto.setPhoneNumber("9999999999");
        dto.setDateOfBirth(LocalDate.of(2000, Month.JANUARY, 1));
        dto.setGender("Female");
        dto.setLinkedinUrl("https://linkedin.com/new");
        dto.setGithubUrl("https://github.com/new");

        service.updateProfile("u1", dto, null);

        assertEquals("newUser", learner.getUsername());
        assertEquals("new@example.com", learner.getEmail());
        assertEquals("New Name", learner.getFullName());
        assertEquals("9999999999", learner.getPhoneNumber());
        assertEquals(LocalDate.of(2000, Month.JANUARY, 1), learner.getDateOfBirth());
        assertEquals("Female", learner.getGender());
        assertEquals("https://linkedin.com/new", learner.getLinkedinUrl());
        assertEquals("https://github.com/new", learner.getGithubUrl());
    }

    // ── deleteProfile edge cases ──────────────────────────────────────────

    @Test
    void deleteProfile_repositoryThrows_throwsUserServiceException() {
        Learner learner = new Learner();
        learner.setUserId("u1");
        when(repository.findByUserId("u1")).thenReturn(learner);
        doThrow(new RuntimeException("DB error")).when(repository).save(any());

        assertThrows(UserServiceException.class, () -> service.deleteProfile("u1"));
    }

    @Test
    void createLearner_exceptionThrown_throwsUserServiceException() {
        UserDTO dto = new UserDTO();
        dto.setUserId("u1");
        doThrow(new RuntimeException("DB down")).when(repository).save(any());
        assertThrows(UserServiceException.class, () -> service.createLearner(dto));
    }

    @Test
    void getProfileByUserId_nullUserId_throwsUserException() {
        assertThrows(UserException.class, () -> service.getProfileByUserId("   "));
    }

    @Test
    void getProfileByUserId_databaseException_throwsUserServiceException() {
        when(repository.findByUserId("u1")).thenThrow(new org.springframework.dao.QueryTimeoutException("Timeout"));
        assertThrows(UserServiceException.class, () -> service.getProfileByUserId("u1"));
    }

    @Test
    void getProfileByUserId_unexpectedException_throwsUserServiceException() {
        when(repository.findByUserId("u1")).thenThrow(new RuntimeException("Unexpected"));
        assertThrows(UserServiceException.class, () -> service.getProfileByUserId("u1"));
    }

    @Test
    void getAllProfiles_databaseException_throwsUserServiceException() {
        when(repository.findAll()).thenThrow(new org.springframework.dao.QueryTimeoutException("Timeout"));
        assertThrows(UserServiceException.class, () -> service.getAllProfiles());
    }

    @Test
    void getAllProfiles_unexpectedException_throwsUserServiceException() {
        when(repository.findAll()).thenThrow(new RuntimeException("Unexpected"));
        assertThrows(UserServiceException.class, () -> service.getAllProfiles());
    }

    @Test
    void updateProfile_nullUserId_throwsUserException() {
        LearnerRequestDTO request = new LearnerRequestDTO();

        assertThrows(
                UserException.class,
                () -> service.updateProfile("   ", request, null)
        );
    }

    @Test
    void updateProfile_databaseException_throwsUserServiceException() {
        LearnerRequestDTO request = new LearnerRequestDTO();

        when(repository.findByUserId("u1"))
                .thenThrow(new RuntimeException("Unexpected"));

        assertThrows(
                UserServiceException.class,
                () -> service.updateProfile("u1", request, null)
        );
    }

    @Test
    void updateProfile_unexpectedException_throwsUserServiceException() {
        LearnerRequestDTO request = new LearnerRequestDTO();

        when(repository.findByUserId("u1"))
                .thenThrow(new RuntimeException("Unexpected"));

        assertThrows(
                UserServiceException.class,
                () -> service.updateProfile("u1", request, null)
        );
    }

    @Test
    void deleteProfile_nullUserId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteProfile("   "));
    }

    @Test
    void deleteProfile_databaseException_throwsUserServiceException() {
        when(repository.findByUserId("u1")).thenThrow(new org.springframework.dao.QueryTimeoutException("Timeout"));
        assertThrows(UserServiceException.class, () -> service.deleteProfile("u1"));
    }

    @Test
    void deleteProfile_unexpectedException_throwsUserServiceException() {
        when(repository.findByUserId("u1")).thenThrow(new RuntimeException("Unexpected"));
        assertThrows(UserServiceException.class, () -> service.deleteProfile("u1"));
    }

    @Test
    void getLearnerNotificationTargets_success() {
        Learner l1 = new Learner(); l1.setUserId("u1"); l1.setEmail("u1@test.com"); l1.setPhoneNumber("1234567890");
        when(repository.findAll()).thenReturn(List.of(l1));

        var targets = service.getLearnerNotificationTargets();
        assertEquals(1, targets.size());
        assertEquals("u1", targets.get(0).getUserId());
        assertEquals("u1@test.com", targets.get(0).getEmail());
    }

    @Test
    void getLearnerNotificationTargets_null_returnsEmptyList() {
        when(repository.findAll()).thenReturn(null);
        var targets = service.getLearnerNotificationTargets();
        assertTrue(targets.isEmpty());
    }

    @Test
    void followTrainer_success() {
        assertDoesNotThrow(() -> service.followTrainer("u1", "t1"));
        verify(followRepository).follow(any());
    }

    @Test
    void followTrainer_nullOrEmptyTrainerId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.followTrainer("u1", null));
        assertThrows(IllegalArgumentException.class, () -> service.followTrainer("u1", "   "));
    }

    @Test
    void unfollowTrainer_success() {
        assertDoesNotThrow(() -> service.unfollowTrainer("u1", "t1"));
        verify(followRepository).unfollow("u1", "t1");
    }

    @Test
    void unfollowTrainer_nullOrEmptyTrainerId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.unfollowTrainer("u1", null));
        assertThrows(IllegalArgumentException.class, () -> service.unfollowTrainer("u1", "   "));
    }

    @Test
    void getFollowedTrainers_success() {
        com.example.user_service.model.LearnerFollow f1 = com.example.user_service.model.LearnerFollow.builder().userId("u1").trainerId("t1").build();
        when(followRepository.findByUserId("u1")).thenReturn(List.of(f1));

        List<String> trainers = service.getFollowedTrainers("u1");
        assertEquals(1, trainers.size());
        assertEquals("t1", trainers.get(0));
    }

    @Test
    void updateProfile_allFieldsAndUpdateOldPicture_success() {
        Learner existing = new Learner();
        existing.setUserId("u1");
        existing.setProfilePictureKey("old-pic.jpg");
        when(repository.findByUserId("u1")).thenReturn(existing);

        LearnerRequestDTO dto = new LearnerRequestDTO();
        dto.setUsername(" newuser ");
        dto.setEmail(" new@test.com ");
        dto.setFullName(" New Name ");
        dto.setPhoneNumber(" 9876543210 ");
        dto.setDateOfBirth(LocalDate.of(1995, Month.MAY, 15));
        dto.setGender(" Male ");
        dto.setLinkedinUrl(" https://linkedin.com/in/user ");
        dto.setGithubUrl(" https://github.com/user ");
        dto.setSelectedSkill(" Java ");
        dto.setPreferredSkill(" Spring ");
        dto.setProfession(" Developer ");
        dto.setSkillLevel(" Advanced ");
        dto.setSkillPriority(" High ");
        dto.setLearningGoal(" Microservices ");
        dto.setTopics(List.of("Java", "Spring Boot"));

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "pic.png", "image/png", "img".getBytes());

        when(fileService.uploadFile(org.mockito.ArgumentMatchers.eq("u1"), any())).thenReturn("new-pic.png");

        LearnerResponseDTO res = service.updateProfile("u1", dto, file);

        assertNotNull(res);
        assertEquals("newuser", existing.getUsername());
        assertEquals("new@test.com", existing.getEmail());
        assertEquals("New Name", existing.getFullName());
        assertEquals("9876543210", existing.getPhoneNumber());
        assertEquals("Male", existing.getGender());
        assertEquals("Developer", existing.getProfession());
        verify(fileService).deleteFile("old-pic.jpg");
    }

    @Test
    void createLearner_unexpectedException_throwsUserServiceException() {
        UserDTO dto = new UserDTO();
        dto.setUserId("u_err");
        doThrow(new RuntimeException("DB error")).when(repository).save(any());
        assertThrows(UserServiceException.class, () -> service.createLearner(dto));
    }

    @Test
    void updateProfile_nullDto_createsNewDtoAndSucceeds() {
        Learner existing = new Learner();
        existing.setUserId("u1");
        when(repository.findByUserId("u1")).thenReturn(existing);

        LearnerResponseDTO res = service.updateProfile("u1", null, null);
        assertNotNull(res);
    }

    @Test
    void getFollowedTrainers_nullFollows_returnsEmptyList() {
        when(followRepository.findByUserId("u1")).thenReturn(null);
        List<String> result = service.getFollowedTrainers("u1");
        assertTrue(result.isEmpty());
    }
}
