package com.example.user_service.service.impl;

import com.example.user_service.commondto.UserDTO;
import com.example.user_service.dto.request.LearnerRequestDTO;
import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.dto.response.UserNotificationTargetDTO;
import com.example.user_service.exception.LearnerNotFoundException;
import com.example.user_service.exception.UserException;
import com.example.user_service.exception.UserServiceException;
import com.example.user_service.feign.AuthClient;
import com.example.user_service.model.Learner;
import com.example.user_service.model.LearnerFollow;
import com.example.user_service.notification.NotificationChannel;
import com.example.user_service.notification.NotificationPublisher;
import com.example.user_service.notification.NotificationRequest;
import com.example.user_service.notification.NotificationType;
import com.example.user_service.repository.LearnerFollowRepository;
import com.example.user_service.repository.LearnerRepository;
import com.example.user_service.service.LearnerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class LearnerServiceImpl implements LearnerService {

    private static final String USER_ID_NULL_MSG = "UserId cannot be null";
    private static final String USER_ID_EMPTY_MSG = "UserId cannot be empty";
    private static final String USER_DTO_NULL_MSG = "UserDTO cannot be null";

    private static final String CREATE_FAILED_MSG = "Failed to create learner. Please try again.";
    private static final String FETCH_FAILED_MSG = "Failed to fetch learner profile. Please try again.";
    private static final String FETCH_ALL_FAILED_MSG = "Failed to fetch learner profiles. Please try again.";
    private static final String UPDATE_FAILED_MSG = "Failed to update learner profile. Please try again.";
    private static final String DELETE_FAILED_MSG = "Failed to delete learner profile. Please try again.";

    private static final String LEARNER_NOT_FOUND_MSG = "Learner not found with id: ";

    private final LearnerRepository repository;
    private final FileServiceImpl fileService;
    private final AuthClient authClient;
    private final NotificationPublisher notificationPublisher;
    private final LearnerFollowRepository followRepository;

    private Clock clock = Clock.systemUTC();

    public LearnerServiceImpl(LearnerRepository repository, FileServiceImpl fileService, AuthClient authClient,
                              NotificationPublisher notificationPublisher, LearnerFollowRepository followRepository) {
        this.repository = repository;
        this.fileService = fileService;
        this.authClient = authClient;
        this.notificationPublisher = notificationPublisher;
        this.followRepository = followRepository;
    }

    public void setClock(Clock clock) {
        if (clock != null) {
            this.clock = clock;
        }
    }


    @Override
    public void createLearner(UserDTO userDTO) {

        Objects.requireNonNull(userDTO, USER_DTO_NULL_MSG);
        Objects.requireNonNull(userDTO.getUserId(), USER_ID_NULL_MSG);

        log.info("Creating learner profile for userId: {}", userDTO.getUserId());

        try {

            Learner learner = new Learner();

            learner.setUserId(userDTO.getUserId());
            learner.setEmail(userDTO.getEmail());
            learner.setFullName(userDTO.getName());
            learner.setUsername(userDTO.getUsername());

            learner.setEmailVerified(Boolean.TRUE.equals(userDTO.getIsVerified()));
            learner.setAccountStatus(Boolean.TRUE.equals(userDTO.getIsActive()));

            learner.setCreatedAt(LocalDateTime.now(clock));
            learner.setUpdatedAt(LocalDateTime.now(clock));

            repository.save(learner);

            log.info("Learner created successfully with userId: {}", learner.getUserId());

            // Send welcome notification to new learner
            sendWelcomeNotificationSafely(learner);

        } catch (IllegalArgumentException e) {
            log.error("Invalid learner data for userId: {}", userDTO.getUserId(), e);
            throw new UserServiceException("Invalid learner data: " + e.getMessage());
        } catch (DataAccessException e) {
            log.error("Database error while creating learner for userId: {}", userDTO.getUserId(), e);
            throw new UserServiceException(CREATE_FAILED_MSG);
        } catch (Exception e) {
            log.error("Unexpected error while creating learner for userId: {}", userDTO.getUserId(), e);
            throw new UserServiceException(CREATE_FAILED_MSG);
        }
    }
    private void sendWelcomeNotificationSafely(Learner learner) {
        try {
            sendWelcomeNotification(learner);
        } catch (Exception ex) {
            log.warn("Failed to send welcome notification for learnerId: {}", learner.getUserId(), ex);
        }
    }
    private void sendWelcomeNotification(Learner learner) {
        NotificationRequest welcomeNotif = NotificationRequest.builder()
                .userId(learner.getUserId())
                .email(learner.getEmail())
                .phoneNumber(learner.getPhoneNumber())
                .title("Welcome to KodeMI! 🎉")
                .message("Hello " +
                        (learner.getFullName() != null ? learner.getFullName() : "there") +
                        "! Welcome to KodeMI. Explore thousands of courses and start your learning journey today!")
                .type(NotificationType.WELCOME_EMAIL)
                .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .referenceId(learner.getUserId())
                .referenceType("USER")
                .build();

        notificationPublisher.publish(welcomeNotif);
    }

    @Override
    public LearnerResponseDTO getProfileByUserId(String userId) {

        Objects.requireNonNull(userId, USER_ID_NULL_MSG);

        if (userId.trim().isEmpty()) {
            throw new UserException(USER_ID_EMPTY_MSG);
        }

        log.info("Fetching learner profile for userId: {}", userId);

        try {

            Learner profile = repository.findByUserId(userId);

            if (profile == null) {
                log.info("Learner profile not found in DB for userId: {}. Attempting fallback from auth-service.", userId);

                createLearnerFromAuthService(userId);

                profile = repository.findByUserId(userId);
            }

            if (profile == null) {
                log.info("Learner profile still missing for userId: {}. Auto-creating default learner record.", userId);

                Learner defaultLearner = new Learner();
                defaultLearner.setUserId(userId);
                defaultLearner.setUsername("User_" + userId.substring(0, Math.min(8, userId.length())));
                defaultLearner.setEmailVerified(true);
                defaultLearner.setAccountStatus(true);
                defaultLearner.setCreatedAt(LocalDateTime.now(clock));
                defaultLearner.setUpdatedAt(LocalDateTime.now(clock));

                repository.save(defaultLearner);
                profile = defaultLearner;
            }

            log.info("Successfully retrieved learner profile for userId: {}", userId);
            return mapToResponseDTO(profile);

        } catch (Exception e) {
            log.error("Failed to fetch learner profile for userId: {}", userId, e);
            throw new UserServiceException(FETCH_FAILED_MSG);
        }
    }

    private void createLearnerFromAuthService(String userId) {
        try {
            List<UserDTO> users = authClient.getUsersByIds(List.of(userId));

            if (users != null && !users.isEmpty()) {
                UserDTO userDTO = users.get(0);

                log.info("Found user in auth-service: {}. Creating learner profile on the fly.",
                        userDTO.getEmail());

                createLearner(userDTO);
            }
        } catch (Exception ex) {
            log.error("Failed to fetch/create learner profile from auth-service fallback", ex);
        }
    }

    @Override
    public List<LearnerResponseDTO> getAllProfiles() {

        log.debug("Fetching all learner profiles");

        try {

            List<Learner> learners = repository.findAll();

            if (learners == null || learners.isEmpty()) {
                return new ArrayList<>();
            }

            List<LearnerResponseDTO> responseDTOs = new ArrayList<>();
            for (Learner learner : learners) {
                if (learner != null) {
                    responseDTOs.add(mapToResponseDTO(learner));
                }
            }
            return responseDTOs;

        } catch (Exception e) {

            log.error("Failed to fetch learner profiles", e);

            throw new UserServiceException(FETCH_ALL_FAILED_MSG);
        }
    }

    public LearnerResponseDTO updateProfile(String userId, LearnerRequestDTO dto, MultipartFile file) {

        Objects.requireNonNull(userId, USER_ID_NULL_MSG);
        if (dto == null) {
            dto = new LearnerRequestDTO();
        }

        if (userId.trim().isEmpty()) {
            throw new UserException(USER_ID_EMPTY_MSG);
        }
        log.info("Updating learner profile for userId: {}", userId);

        try {

            Learner profile = repository.findByUserId(userId);

            if (profile == null) {
                log.info("Learner profile missing during update for userId: {}. Creating on the fly.", userId);
                profile = new Learner();
                profile.setUserId(userId);
                profile.setCreatedAt(LocalDateTime.now(clock));
                profile.setEmailVerified(true);
                profile.setAccountStatus(true);
            }
            updateLearnerFields(dto, profile);
            if (file != null){
                String oldKey = profile.getProfilePictureKey();
                String key = fileService.uploadFile(userId,file);
                profile.setProfilePictureKey(key);
                if(oldKey != null) {
                    fileService.deleteFile(oldKey);
                }
            }
            repository.save(profile);
            log.info("Learner profile updated successfully for userId: {}", userId);

            return mapToResponseDTO(profile);

        } catch (LearnerNotFoundException e) {

            throw e;

        } catch (Exception e) {

            log.error("Failed to update learner profile for userId: {}", userId, e);
            throw new UserServiceException(UPDATE_FAILED_MSG);
        }
    }

    @Override
    @CacheEvict(value = "learnerDetails", key = "#userId")
    public void deleteProfile(String userId) {

        Objects.requireNonNull(userId, USER_ID_NULL_MSG);

        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException(USER_ID_EMPTY_MSG);
        }

        log.info("Deactivating learner profile for userId: {}", userId);

        try {

            Learner learner = repository.findByUserId(userId);

            if (learner == null) {
                throw new LearnerNotFoundException(LEARNER_NOT_FOUND_MSG + userId);
            }

            // mark as inactive
            learner.setAccountStatus(false);

            repository.save(learner);

            log.info("Learner profile marked inactive for userId: {}", userId);

        } catch (LearnerNotFoundException e) {

            throw e;

        } catch (Exception e) {

            log.error("Failed to deactivate learner profile for userId: {}", userId, e);

            throw new UserServiceException(DELETE_FAILED_MSG);
        }
    }
    @Override
    public List<UserNotificationTargetDTO> getLearnerNotificationTargets() {
        List<Learner> learners = repository.findAll();

        List<UserNotificationTargetDTO> targets = new ArrayList<>();
        if (learners != null) {
            for (Learner learner : learners) {
                if (learner != null) {
                    targets.add(UserNotificationTargetDTO.builder()
                            .userId(learner.getUserId())
                            .email(learner.getEmail())
                            .phoneNumber(learner.getPhoneNumber())
                            .build());
                }
            }
        }
        return targets;
    }

    private LearnerResponseDTO mapToResponseDTO(Learner profile) {

        LearnerResponseDTO response = new LearnerResponseDTO();
        response.setUsername(profile.getUsername());
        response.setEmail(profile.getEmail());
        response.setProfilePictureUrl(fileService.generatePresignedUrl(profile.getProfilePictureKey()));
        response.setFullName(profile.getFullName());
        response.setPhoneNumber(profile.getPhoneNumber());
        response.setDateOfBirth(profile.getDateOfBirth());
        response.setGender(profile.getGender());
        response.setLinkedinUrl(profile.getLinkedinUrl());
        response.setGithubUrl(profile.getGithubUrl());
        response.setSelectedSkill(profile.getSelectedSkill());
        response.setPreferredSkill(profile.getPreferredSkill());
        response.setProfession(profile.getProfession());
        response.setSkillLevel(profile.getSkillLevel());
        response.setSkillPriority(profile.getSkillPriority());
        response.setLearningGoal(profile.getLearningGoal());
        response.setTopics(profile.getTopics());
        response.setEmailVerified(profile.getEmailVerified());
        response.setAccountStatus(profile.isAccountStatus());
        response.setUpdatedAt(profile.getUpdatedAt());

        return response;
    }

    private void updateLearnerFields(LearnerRequestDTO dto, Learner profile) {

        LocalDateTime now = LocalDateTime.now(clock);

        if (dto.getUsername() != null) {
            profile.setUsername(dto.getUsername().trim());
        }

        if (dto.getEmail() != null) {
            profile.setEmail(dto.getEmail().trim());
        }

        if (dto.getFullName() != null) {
            profile.setFullName(dto.getFullName().trim());
        }

        if (dto.getPhoneNumber() != null) {
            profile.setPhoneNumber(dto.getPhoneNumber().trim());
        }

        if (dto.getDateOfBirth() != null) {
            profile.setDateOfBirth(dto.getDateOfBirth());
        }

        if (dto.getGender() != null) {
            profile.setGender(dto.getGender().trim());
        }

        if (dto.getLinkedinUrl() != null) {
            profile.setLinkedinUrl(dto.getLinkedinUrl().trim());
        }

        if (dto.getGithubUrl() != null) {
            profile.setGithubUrl(dto.getGithubUrl().trim());
        }

        if (dto.getSelectedSkill() != null) {
            profile.setSelectedSkill(dto.getSelectedSkill().trim());
        }

        if (dto.getPreferredSkill() != null) {
            profile.setPreferredSkill(dto.getPreferredSkill().trim());
        }

        if (dto.getProfession() != null) {
            profile.setProfession(dto.getProfession().trim());
        }

        if (dto.getSkillLevel() != null) {
            profile.setSkillLevel(dto.getSkillLevel().trim());
        }

        if (dto.getSkillPriority() != null) {
            profile.setSkillPriority(dto.getSkillPriority().trim());
        }

        if (dto.getLearningGoal() != null) {
            profile.setLearningGoal(dto.getLearningGoal().trim());
        }

        if (dto.getTopics() != null) {
            profile.setTopics(dto.getTopics());
        }

        profile.setUpdatedAt(now);
    }

    @Override
    public void followTrainer(String userId, String trainerId) {
        Objects.requireNonNull(userId, USER_ID_NULL_MSG);
        if (trainerId == null || trainerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trainer ID cannot be null or empty");
        }
        LearnerFollow follow = LearnerFollow.builder()
                .userId(userId)
                .trainerId(trainerId.trim())
                .createdAt(LocalDateTime.now(clock).toString())
                .build();
        followRepository.follow(follow);
        log.info("Learner {} followed trainer {}", userId, trainerId);
    }

    @Override
    public void unfollowTrainer(String userId, String trainerId) {
        Objects.requireNonNull(userId, USER_ID_NULL_MSG);
        if (trainerId == null || trainerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trainer ID cannot be null or empty");
        }
        followRepository.unfollow(userId, trainerId.trim());
        log.info("Learner {} unfollowed trainer {}", userId, trainerId);
    }

    @Override
    public List<String> getFollowedTrainers(String userId) {
        Objects.requireNonNull(userId, USER_ID_NULL_MSG);
        List<LearnerFollow> follows = followRepository.findByUserId(userId);
        List<String> trainerIds = new ArrayList<>();
        if (follows != null) {
            for (LearnerFollow f : follows) {
                if (f.getTrainerId() != null) {
                    trainerIds.add(f.getTrainerId());
                }
            }
        }
        return trainerIds;
    }
}