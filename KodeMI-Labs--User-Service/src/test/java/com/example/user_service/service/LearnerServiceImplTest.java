package com.example.user_service.service;

import com.example.user_service.commondto.UserDTO;
import com.example.user_service.dto.request.LearnerRequestDTO;
import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.exception.LearnerNotFoundException;
import com.example.user_service.exception.UserException;
import com.example.user_service.model.Learner;
import com.example.user_service.notification.NotificationPublisher;
import com.example.user_service.repository.LearnerFollowRepository;
import com.example.user_service.repository.LearnerRepository;
import com.example.user_service.service.impl.FileServiceImpl;
import com.example.user_service.service.impl.LearnerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearnerServiceImplTest {

    @Mock
    private LearnerRepository repository;

    @Mock
    private FileServiceImpl fileService; // mock the dependency

    @Mock
    private com.example.user_service.feign.AuthClient authClient;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private LearnerFollowRepository followRepository;

    @InjectMocks
    private LearnerServiceImpl service;

    private Clock clock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        clock = Clock.fixed(
                Instant.parse("2026-07-27T10:00:00Z"),
                ZoneOffset.UTC
        );

        service.setClock(clock);
    }

    // ========== createLearner ==========
    @Test
    void createLearner_success() {
        UserDTO dto = new UserDTO();
        dto.setUserId("user123");
        dto.setEmail("test@example.com");
        dto.setName("Test User");
        dto.setUsername("testuser");
        dto.setIsVerified(true);
        dto.setIsActive(true);

        service.createLearner(dto);

        ArgumentCaptor<Learner> captor = ArgumentCaptor.forClass(Learner.class);
        verify(repository, times(1)).save(captor.capture());

        Learner saved = captor.getValue();
        assertEquals("user123", saved.getUserId());
        assertEquals("test@example.com", saved.getEmail());
        assertEquals("Test User", saved.getFullName());
        assertTrue(saved.getEmailVerified());
        assertTrue(saved.isAccountStatus());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void createLearner_nullUserDTO_throwsException() {
        assertThrows(NullPointerException.class, () -> service.createLearner(null));
    }

    // ========== getProfileByUserId ==========
    @Test
    void getProfileByUserId_success() {
        Learner learner = new Learner();
        learner.setUserId("user123");
        learner.setUsername("testuser");
        learner.setProfilePictureKey("testkey");
        when(repository.findByUserId("user123")).thenReturn(learner);
        when(fileService.generatePresignedUrl("testkey")).thenReturn("https://presigned.url");

        LearnerResponseDTO dto = service.getProfileByUserId("user123");

        assertEquals("testuser", dto.getUsername());
        assertEquals("https://presigned.url", dto.getProfilePictureUrl());
        verify(repository, times(1)).findByUserId("user123");
        verify(fileService, times(1)).generatePresignedUrl("testkey");
    }

    @Test
    void getProfileByUserId_notFound_createsDefaultProfileOnTheFly() {
        when(repository.findByUserId("user123")).thenReturn(null);
        LearnerResponseDTO result = service.getProfileByUserId("user123");
        assertNotNull(result);
        assertEquals("User_user123", result.getUsername());
    }

    @Test
    void getProfileByUserId_nullOrEmpty_throwsException() {
        assertThrows(NullPointerException.class, () -> service.getProfileByUserId(null));
        assertThrows(UserException.class, () -> service.getProfileByUserId(" "));
    }

    // ========== getAllProfiles ==========
    @Test
    void getAllProfiles_success() {
        Learner learner1 = new Learner();
        learner1.setUsername("user1");
        Learner learner2 = new Learner();
        learner2.setUsername("user2");
        when(repository.findAll()).thenReturn(Arrays.asList(learner1, learner2));

        List<LearnerResponseDTO> list = service.getAllProfiles();

        assertEquals(2, list.size());
        assertEquals("user1", list.get(0).getUsername());
        assertEquals("user2", list.get(1).getUsername());
    }

    // ========== updateProfile ==========
    @Test
    void updateProfile_successWithoutFile() {
        Learner learner = new Learner();
        learner.setUserId("user123");
        when(repository.findByUserId("user123")).thenReturn(learner);

        LearnerRequestDTO dto = new LearnerRequestDTO();
        dto.setFullName("Updated Name");
        dto.setEmail("updated@example.com");

        LearnerResponseDTO response = service.updateProfile("user123", dto, null);

        assertEquals("Updated Name", response.getFullName());
        assertEquals("updated@example.com", response.getEmail());
        verify(repository, times(1)).save(learner);
    }

    @Test
    void updateProfile_successWithFile() {
        Learner learner = new Learner();
        learner.setUserId("user123");
        learner.setProfilePictureKey("oldKey");
        when(repository.findByUserId("user123")).thenReturn(learner);

        LearnerRequestDTO dto = new LearnerRequestDTO();
        dto.setFullName("Updated Name");

        MultipartFile file = mock(MultipartFile.class);
        when(fileService.uploadFile("user123", file)).thenReturn("newKey");

        LearnerResponseDTO response = service.updateProfile("user123", dto, file);

        assertEquals("Updated Name", response.getFullName());
        assertEquals("newKey", learner.getProfilePictureKey());
        verify(fileService).deleteFile("oldKey"); // deletes the OLD key, not the new one
        verify(fileService).uploadFile("user123", file);
        verify(repository, times(1)).save(learner);
    }

    @Test
    void updateProfile_notFound_createsProfileOnTheFly() {
        when(repository.findByUserId("user123")).thenReturn(null);
        LearnerRequestDTO dto = new LearnerRequestDTO();
        dto.setFullName("On The Fly User");
        LearnerResponseDTO response = service.updateProfile("user123", dto, null);
        assertNotNull(response);
        assertEquals("On The Fly User", response.getFullName());
    }

    // ========== deleteProfile ==========
    @Test
    void deleteProfile_success() {
        Learner learner = new Learner();
        learner.setUserId("user123");
        learner.setAccountStatus(true);
        when(repository.findByUserId("user123")).thenReturn(learner);

        service.deleteProfile("user123");

        assertFalse(learner.isAccountStatus());
        verify(repository, times(1)).save(learner);
    }

    @Test
    void deleteProfile_notFound_throwsException() {
        when(repository.findByUserId("user123")).thenReturn(null);
        assertThrows(LearnerNotFoundException.class, () -> service.deleteProfile("user123"));
    }

    @Test
    void deleteProfile_nullOrEmpty_throwsException() {
        assertThrows(NullPointerException.class, () -> service.deleteProfile(null));
        assertThrows(IllegalArgumentException.class, () -> service.deleteProfile(" "));
    }

    // ========== follow / unfollow ==========
    @Test
    void followTrainer_success() {
        service.followTrainer("user123", "trainer456");
        verify(followRepository, times(1)).follow(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void followTrainer_blankTrainerId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.followTrainer("user123", " "));
    }

    @Test
    void unfollowTrainer_success() {
        service.unfollowTrainer("user123", "trainer456");
        verify(followRepository, times(1)).unfollow("user123", "trainer456");
    }

    @Test
    void getFollowedTrainers_success() {
        com.example.user_service.model.LearnerFollow follow = com.example.user_service.model.LearnerFollow.builder()
                .userId("user123")
                .trainerId("trainer456")
                .build();
        when(followRepository.findByUserId("user123")).thenReturn(List.of(follow));

        List<String> trainerIds = service.getFollowedTrainers("user123");

        assertEquals(1, trainerIds.size());
        assertEquals("trainer456", trainerIds.get(0));
    }
}