package com.example.user_service.service.impl;

import com.example.user_service.dto.request.TrainerRequestDTO;
import com.example.user_service.dto.response.TrainerAdminResponse;
import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.dto.response.TrainerUserResponseDTO;
import com.example.user_service.dto.update.TrainerUpdateRequestDTO;
import com.example.user_service.exception.TrainerNotFoundException;
import com.example.user_service.exception.UserException;
import com.example.user_service.exception.UserServiceException;
import com.example.user_service.feign.AuthClient;
import com.example.user_service.model.Trainer;
import com.example.user_service.notification.NotificationChannel;
import com.example.user_service.notification.NotificationPublisher;
import com.example.user_service.notification.NotificationRequest;
import com.example.user_service.notification.NotificationType;
import com.example.user_service.repository.TrainerRepository;
import com.example.user_service.service.TrainerService;
import com.example.user_service.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.user_service.commondto.UserDTO;

@Slf4j
@Service
public class TrainerServiceImpl implements TrainerService {

    private static final String USER_ID_NULL_MSG = "UserId cannot be null";
    private static final String USER_ID_EMPTY_MSG = "UserId cannot be empty";
    private static final String TRAINER_ALREADY_EXISTS = "Trainer Already Registered.";
    private static final String TRAINER_REGISTERED_SUCCESS = "Trainer Registered Successfully.";
    private static final String TRAINER_UPDATED_SUCCESS = "Trainer Updated Successfully";
    private static final String TRAINER_DELETED_SUCCESS = "Trainer Deleted successfully";
    private static final String TRAINER_NOT_FOUND_MSG = "Trainer not found with id: ";
    private static final String UPDATE_FAILED_MSG = "Failed to update trainer profile. Please try again.";
    private static final String DELETE_FAILED_MSG = "Failed to delete trainer profile. Please try again.";
    private static final String FETCH_ALL_FAILED_MSG = "Failed to fetch trainers. Please try again.";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_APPROVAL_PENDING = "APPROVAL_PENDING";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final TrainerRepository repository;
    private final AuthClient authClient;
    private final FileServiceImpl fileService;
    private final NotificationPublisher notificationPublisher;

    private Clock clock = Clock.systemUTC();


    public TrainerServiceImpl(TrainerRepository repository,
                              AuthClient authClient,
                              FileServiceImpl fileService,
                              NotificationPublisher notificationPublisher,
                              Clock clock) {
        this.repository = repository;
        this.authClient = authClient;
        this.fileService = fileService;
        this.notificationPublisher = notificationPublisher;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public void setClock(Clock clock) {
        if (clock != null) {
            this.clock = clock;
        }
    }

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = {"allTrainers", "allPendingTrainers", "allTrainersForUsers"}, allEntries = true)
    public String createTrainerProfile(
            TrainerRequestDTO request,
            com.example.user_service.dto.request.TrainerProfileFiles files,
            String userId,
            String token) {

        Objects.requireNonNull(request, "TrainerRequestDTO cannot be null");
        Objects.requireNonNull(userId, USER_ID_NULL_MSG);
        Objects.requireNonNull(files, "TrainerProfileFiles cannot be null");

        MultipartFile demo = files.demo();
        MultipartFile profileImage = files.profileImage();
        MultipartFile globalCertifications = files.globalCertifications();
        MultipartFile totRegistration = files.totRegistration();
        MultipartFile supportingDocuments = files.supportingDocuments();

        Objects.requireNonNull(demo, "Demo cannot be null");

        if (userId.trim().isEmpty()) {
            throw new UserException(USER_ID_EMPTY_MSG);
        }

        Trainer existingTrainer = repository.findById(userId);
        if (existingTrainer != null) {
            log.warn("Trainer already exists for userId: {}", userId);
            return TRAINER_ALREADY_EXISTS;
        }

        Trainer trainer = new Trainer();
        trainer.setUserId(userId);
        trainer.setCreatedAt(LocalDateTime.now(clock));
        trainer.setUpdatedAt(LocalDateTime.now(clock));
        String key = fileService.uploadFile(userId, demo);
        trainer.setDemoContentKey(key);

        if (profileImage != null && !profileImage.isEmpty()) {
            trainer.setProfileImageURL(fileService.uploadFile(userId, profileImage));
        }
        if (globalCertifications != null && !globalCertifications.isEmpty()) {
            trainer.setGlobalCertifications(fileService.uploadFile(userId, globalCertifications));
        }
        if (totRegistration != null && !totRegistration.isEmpty()) {
            trainer.setTopRegistration(fileService.uploadFile(userId, totRegistration));
        }
        if (supportingDocuments != null && !supportingDocuments.isEmpty()) {
            trainer.setSupportingDocumentsChecklist(fileService.uploadFile(userId, supportingDocuments));
        }

        mapRequestToEntity(request, trainer);

        try {
            authClient.updateUserRole(userId, "TRAINER", token);

            repository.save(trainer);
            log.info("Trainer saved in trainer table: {}", userId);

            authClient.markTrainerSubmitted(userId, token);
            log.info("Auth-service notified, status=APPROVAL_PENDING: {}", userId);
            log.info("Admin-service notified for new trainer application: {}", userId);

            sendTrainerRegisteredNotification(userId, trainer);

            return TRAINER_REGISTERED_SUCCESS;

        } catch (Exception ex) {
            log.error("Failed to create trainer for userId: {}. Reason: {}", userId, ex.getMessage(), ex);
            throw new UserServiceException("Failed to create trainer profile. UserId: " + userId);
        }
    }

    private void sendTrainerRegisteredNotification(String userId, Trainer trainer) {
        try {
            NotificationRequest notif = NotificationRequest.builder()
                    .userId(userId)
                    .email(trainer.getEmail())
                    .phoneNumber(trainer.getPhoneNumber())
                    .title("Trainer Application Submitted 🚀")
                    .message("Your trainer application has been submitted successfully! Our team will review it and get back to you within 2-3 business days.")
                    .type(NotificationType.TRAINER_REGISTERED)
                    .channels(java.util.List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                    .referenceId(userId)
                    .referenceType("USER")
                    .build();
            notificationPublisher.publish(notif);
        } catch (Exception ex) {
            log.warn("Failed to send TRAINER_REGISTERED notification for userId: {}", userId, ex);
        }
    }


    @Override
    @org.springframework.cache.annotation.Cacheable(value = "trainerDetails", key = "#userId")
    public TrainerResponseDTO getTrainerProfileById(String userId) {
        log.info("Fetching trainer profile for userId: {}", userId);
        Objects.requireNonNull(userId, USER_ID_NULL_MSG);
        if (userId.trim().isEmpty()) {
            throw new UserException(USER_ID_EMPTY_MSG);
        }
        Trainer trainer = repository.findById(userId);
        if (trainer == null) {
            log.warn("Trainer profile not found in DB for userId: {}. Attempting fallback from auth-service.", userId);
            trainer = fetchOrCreateBasicTrainerFromAuthService(userId);
        }
        if (trainer == null) {
            log.warn("Trainer not found in database or fallback for userId: {}", userId);
            throw new TrainerNotFoundException(TRAINER_NOT_FOUND_MSG + userId);
        }
        log.info("Successfully retrieved trainer profile for userId: {}", userId);
        return mapEntityToResponse(trainer);
    }

    private Trainer fetchOrCreateBasicTrainerFromAuthService(String userId) {
        try {
            List<UserDTO> users = authClient.getUsersByIds(List.of(userId));
            if (users != null && !users.isEmpty()) {
                UserDTO userDTO = users.get(0);
                log.info("Found trainer in auth-service: {}. Creating basic trainer profile.", userDTO.getEmail());
                Trainer newTrainer = new Trainer();
                newTrainer.setUserId(userId);
                newTrainer.setEmail(userDTO.getEmail());
                newTrainer.setFullName(userDTO.getName());
                newTrainer.setCreatedAt(LocalDateTime.now(clock));
                newTrainer.setUpdatedAt(LocalDateTime.now(clock));
                repository.save(newTrainer);
                return repository.findById(userId);
            }
        } catch (Exception e) {
            log.error("Failed to fetch/create basic trainer profile from auth-service fallback: {}", e.getMessage());
        }
        return null;
    }

    @Override
    @org.springframework.cache.annotation.Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(value = "trainerDetails", key = "#userId"),
            @org.springframework.cache.annotation.CacheEvict(value = {"allTrainers", "allPendingTrainers", "allTrainersForUsers"}, allEntries = true)
    })
    public String updateTrainerProfile(String userId, TrainerUpdateRequestDTO request, MultipartFile file) {
        Objects.requireNonNull(userId, USER_ID_NULL_MSG);
        if (request == null) {
            request = new TrainerUpdateRequestDTO();
        }
        if (userId.trim().isEmpty()) {
            throw new IllegalArgumentException(USER_ID_EMPTY_MSG);
        }
        Trainer trainer = repository.findById(userId);
        if (trainer == null) {
            log.warn("Trainer not found for update with userId: {}", userId);
            throw new TrainerNotFoundException(TRAINER_NOT_FOUND_MSG + userId);
        }
        try {
            if (file != null && !file.isEmpty()) {
                String key = fileService.uploadFile(userId, file);
                if (trainer.getProfileImageURL() != null) {
                    fileService.deleteFile(trainer.getProfileImageURL());
                }
                trainer.setProfileImageURL(key);
            }
            updateTrainerFieldsPlain(request, trainer);
            repository.save(trainer);
            log.info("Trainer profile updated successfully for userId: {}", userId);
            return TRAINER_UPDATED_SUCCESS;
        } catch (Exception ex) {
            log.error("Failed to update trainer profile for userId: {}. Reason: {}", userId, ex.getMessage(), ex);
            throw new UserServiceException(UPDATE_FAILED_MSG + " UserId: " + userId);
        }
    }

    @Override
    @org.springframework.cache.annotation.Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(value = "trainerDetails", key = "#userId"),
            @org.springframework.cache.annotation.CacheEvict(value = {"allTrainers", "allPendingTrainers", "allTrainersForUsers"}, allEntries = true)
    })
    public String deleteTrainerProfile(String userId) {
        Objects.requireNonNull(userId, USER_ID_NULL_MSG);
        if (userId.trim().isEmpty()) {
            throw new UserException(USER_ID_EMPTY_MSG);
        }
        try {
            repository.delete(userId);
            log.info("Trainer profile deleted successfully for userId: {}", userId);
            return TRAINER_DELETED_SUCCESS;
        } catch (Exception ex) {
            log.error("Failed to delete trainer profile for userId: {}. Reason: {}", userId, ex.getMessage(), ex);
            throw new UserServiceException(DELETE_FAILED_MSG + " UserId: " + userId);
        }
    }

    @org.springframework.cache.annotation.Cacheable(value = "allPendingTrainers", key = "'pending'")
    public List<TrainerResponseDTO> getAllPendingTrainers(){
        List<String> userIds = authClient.getPendingTrainers();
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Trainer> trainers = repository.findByIds(userIds);
        List<TrainerResponseDTO> responseDTOs = new ArrayList<>();
        if (trainers != null) {
            for (Trainer trainer : trainers) {
                if (trainer != null) {
                    responseDTOs.add(mapEntityToResponse(trainer));
                }
            }
        }
        return responseDTOs;
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "allTrainers", key = "'all'")
    public List<TrainerResponseDTO> getAllTrainers() {
        try {
            List<Trainer> trainers = repository.findAll();
            if (trainers == null || trainers.isEmpty()) return new ArrayList<>();

            Map<String, UserDTO> userMap = fetchUserMapForTrainers(trainers);

            List<TrainerResponseDTO> responseList = new ArrayList<>();
            for (Trainer trainer : trainers) {
                if (trainer != null) {
                    TrainerResponseDTO dto = mapEntityToResponse(trainer);
                    UserDTO user = userMap.get(trainer.getUserId());
                    populateTrainerStatus(dto, user);
                    responseList.add(dto);
                }
            }
            log.info("Fetched all trainers with statuses successfully.");
            return responseList;
        } catch (Exception ex) {
            log.error("Failed to fetch all trainers. Reason: {}", ex.getMessage(), ex);
            throw new UserServiceException(FETCH_ALL_FAILED_MSG);
        }
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "allTrainersForUsers", key = "'users'")
    public List<TrainerUserResponseDTO> getAllTrainersForUsers() {
        try {
            List<Trainer> trainers = repository.findAll();
            if (trainers == null || trainers.isEmpty()) return new ArrayList<>();

            Map<String, UserDTO> userMap = fetchUserMapForTrainers(trainers);

            List<TrainerUserResponseDTO> responseList = new ArrayList<>();
            for (Trainer trainer : trainers) {
                if (trainer != null) {
                    UserDTO user = userMap.get(trainer.getUserId());
                    if (user != null && STATUS_ACTIVE.equals(user.getStatus())) {
                        TrainerUserResponseDTO dto = mapEntityToUserResponse(trainer);
                        dto.setIsVerified(true);
                        responseList.add(dto);
                    }
                }
            }
            log.info("Fetched all active trainers for users successfully.");
            return responseList;
        } catch (Exception ex) {
            log.error("Failed to fetch all trainers for users. Reason: {}", ex.getMessage(), ex);
            throw new UserServiceException(FETCH_ALL_FAILED_MSG);
        }
    }

    private Map<String, UserDTO> fetchUserMapForTrainers(List<Trainer> trainers) {
        if (trainers == null || trainers.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> userIds = trainers.stream()
                .filter(t -> t != null && t.getUserId() != null)
                .map(Trainer::getUserId)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserDTO> users = authClient.getUsersByIds(userIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        return users.stream()
                .filter(u -> u != null && u.getUserId() != null)
                .collect(Collectors.toMap(UserDTO::getUserId, u -> u, (existing, replacement) -> existing));
    }

    private void populateTrainerStatus(TrainerResponseDTO dto, UserDTO user) {
        if (user != null && user.getStatus() != null) {
            dto.setIsVerified(false);

            switch (user.getStatus()) {
                case STATUS_APPROVAL_PENDING:
                    dto.setStatus(STATUS_PENDING);
                    break;

                case STATUS_ACTIVE:
                    dto.setStatus(STATUS_APPROVED);
                    dto.setIsVerified(true);
                    break;

                case STATUS_REJECTED:
                    dto.setStatus(STATUS_REJECTED);
                    break;

                default:
                    dto.setStatus(STATUS_PENDING);
                    break;
            }
        }
    }


    private void mapRequestToEntity(TrainerRequestDTO request, Trainer trainer) {

        trainer.setFullName(request.getFullName());
        trainer.setDesignation(request.getDesignation());
        trainer.setPhoneNumber(request.getPhoneNumber());
        trainer.setEmail(request.getEmail());
        trainer.setGender(request.getGender());
        trainer.setLanguageKnown(request.getLanguageKnown());
        trainer.setOfficeName(request.getOfficeName());
        trainer.setOfficeAddress(request.getOfficeAddress());

        trainer.setLinkedInOrWebsiteURL(request.getLinkedInOrWebsiteURL());
        trainer.setGitHubUrl(request.getGithubUrl());

        trainer.setTrainingSpecialization(request.getTrainingSpecialization());
        trainer.setYearsOfExperience(request.getYearsOfExperience());
        trainer.setQualification(request.getQualification());
        trainer.setDateOfBirth(request.getDateOfBirth());

        trainer.setModesOfTrainingPreferred(request.getModesOfTrainingPreferred());
        trainer.setClientsTrainedBefore(request.getClientsTrainedBefore());
        trainer.setPanNumber(request.getPanNumber());
        trainer.setBankName(request.getBankName());
        trainer.setBranchName(request.getBranchName());
        trainer.setAccountNumber(request.getAccountNumber());
        trainer.setIfscCode(request.getIfscCode());

        trainer.setGlobalCertifications(request.getGlobalCertifications());
        trainer.setTopRegistration(request.getTopRegistration());
        trainer.setSupportingDocumentsChecklist(request.getSupportingDocumentsChecklist());

        trainer.setAnyLegalDisputesInPast5Years(request.getAnyLegalDisputesInPast5Years());
    }

    private TrainerResponseDTO mapEntityToResponse(Trainer trainer) {
        String url = fileService.generatePresignedUrl(trainer.getDemoContentKey());
        TrainerResponseDTO response = new TrainerResponseDTO();
        if(trainer.getResumeKey() != null) {
            String rUrl = fileService.generatePresignedUrl(trainer.getResumeKey());
            response.setResumeUrl(rUrl);
        }
        if(trainer.getProfileImageURL() != null){
            String pUrl = fileService.generatePresignedUrl(trainer.getProfileImageURL());
            response.setProfilePictureURL(pUrl);
        }
        response.setUserId(trainer.getUserId());
        response.setFullName(trainer.getFullName());
        response.setEmailId(trainer.getEmail());
        response.setRating(trainer.getRatingValue());
        response.setContentUrl(url);
        response.setGender(trainer.getGender());
        response.setLanguageKnown(trainer.getLanguageKnown());
        response.setDateOfBirth(trainer.getDateOfBirth());
        response.setDesignation(trainer.getDesignation());
        response.setPhoneNumber(trainer.getPhoneNumber());
        response.setOfficeName(trainer.getOfficeName());
        response.setOfficeAddress(trainer.getOfficeAddress());
        response.setLinkedInOrWebsiteURL(trainer.getLinkedInOrWebsiteURL());
        response.setGithubURL(trainer.getGitHubUrl());
        response.setTrainingSpecialization(trainer.getTrainingSpecialization());
        response.setYearsOfExperience(trainer.getYearsOfExperience());
        response.setQualification(trainer.getQualification());
        response.setModesOfTrainingPreferred(trainer.getModesOfTrainingPreferred());
        response.setClientsTrainedBefore(trainer.getClientsTrainedBefore());
        response.setCreatedAt(trainer.getCreatedAt());
        response.setUpdatedAt(trainer.getUpdatedAt());
        response.setGlobalCertifications(trainer.getGlobalCertifications());
        response.setTopRegistration(trainer.getTopRegistration());
        response.setSupportingDocumentsChecklist(trainer.getSupportingDocumentsChecklist());
        response.setAnyLegalDisputesInPast5Years(trainer.getAnyLegalDisputesInPast5Years());
        return response;
    }
    private TrainerAdminResponse mapEntityToAdminResponse(Trainer trainer) {
        String url = fileService.generatePresignedUrl(trainer.getDemoContentKey());
        TrainerAdminResponse response = new TrainerAdminResponse();
        if(trainer.getResumeKey() != null) {
            String rUrl = fileService.generatePresignedUrl(trainer.getResumeKey());
            response.setResumeKey(rUrl);
        }
        if(trainer.getProfileImageURL() != null){
            String pUrl = fileService.generatePresignedUrl(trainer.getProfileImageURL());
            response.setProfileImageURL(pUrl);
        }
        response.setUserId(trainer.getUserId());
        response.setFullName(trainer.getFullName());
        response.setEmail(trainer.getEmail());
        response.setRatingValue(trainer.getRatingValue());
        response.setDemoContentKey(url);
        response.setGender(trainer.getGender());
        response.setLanguageKnown(trainer.getLanguageKnown());
        response.setDateOfBirth(trainer.getDateOfBirth());
        response.setDesignation(trainer.getDesignation());
        response.setPhoneNumber(trainer.getPhoneNumber());
        response.setOfficeName(trainer.getOfficeName());
        response.setOfficeAddress(trainer.getOfficeAddress());
        response.setLinkedInOrWebsiteURL(trainer.getLinkedInOrWebsiteURL());
        response.setGitHubUrl(trainer.getGitHubUrl());
        response.setTrainingSpecialization(trainer.getTrainingSpecialization());
        response.setYearsOfExperience(trainer.getYearsOfExperience());
        response.setQualification(trainer.getQualification());
        response.setModesOfTrainingPreferred(trainer.getModesOfTrainingPreferred());
        response.setClientsTrainedBefore(trainer.getClientsTrainedBefore());
        response.setCreatedAt(trainer.getCreatedAt());
        response.setUpdatedAt(trainer.getUpdatedAt());
        response.setGlobalCertifications(trainer.getGlobalCertifications());
        response.setTopRegistration(trainer.getTopRegistration());
        response.setSupportingDocumentsChecklist(trainer.getSupportingDocumentsChecklist());
        response.setAnyLegalDisputesInPast5Years(trainer.getAnyLegalDisputesInPast5Years());
        if (trainer.getPanNumber() != null)
            response.setPanNumber(EncryptionUtil.decrypt(trainer.getPanNumber().trim()));
        if (trainer.getBankName() != null)
            response.setBankName(trainer.getBankName().trim());
        if (trainer.getBranchName() != null)
            response.setBranchName(trainer.getBranchName().trim());
        if (trainer.getAccountNumber() != null)
            response.setAccountNumber(EncryptionUtil.decrypt(trainer.getAccountNumber().trim()));
        if (trainer.getIfscCode() != null)
            response.setIfscCode(EncryptionUtil.decrypt(trainer.getIfscCode().trim()));
        return response;
    }

    private TrainerUserResponseDTO mapEntityToUserResponse(Trainer trainer) {
        String url = fileService.generatePresignedUrl(trainer.getDemoContentKey());
        String profileUrl = fileService.generatePresignedUrl(trainer.getProfileImageURL());
        TrainerUserResponseDTO response = new TrainerUserResponseDTO();
        response.setUserId(trainer.getUserId());
        response.setFullName(trainer.getFullName());
        response.setDesignation(trainer.getDesignation());
        response.setPhoneNumber(trainer.getPhoneNumber());
        response.setEmailId(trainer.getEmail());
        response.setLanguageKnown(trainer.getLanguageKnown());
        response.setOfficeName(trainer.getOfficeName());
        response.setContentUrl(url);
        response.setRating(trainer.getRatingValue());
        response.setOfficeAddress(trainer.getOfficeAddress());
        response.setLinkedInURL(trainer.getLinkedInOrWebsiteURL());
        response.setGithubURL(trainer.getGitHubUrl());
        response.setTrainingSpecialization(trainer.getTrainingSpecialization());
        response.setYearsOfExperience(trainer.getYearsOfExperience());
        response.setQualification(trainer.getQualification());
        response.setModesOfTrainingPreferred(trainer.getModesOfTrainingPreferred());
        response.setClientsTrainedBefore(trainer.getClientsTrainedBefore());
        response.setProfilePictureURL(profileUrl);
        response.setDateOfBirth(trainer.getDateOfBirth());
        response.setGender(trainer.getGender());
        response.setGlobalCertifications(trainer.getGlobalCertifications());
        response.setTopRegistration(trainer.getTopRegistration());
        response.setCreatedAt(trainer.getCreatedAt());
        response.setUpdatedAt(trainer.getUpdatedAt());
        return response;
    }

    private void updateTrainerFieldsPlain(TrainerUpdateRequestDTO dto, Trainer trainer) {
        LocalDateTime now = LocalDateTime.now(clock);
        updateBasicInfo(dto, trainer);
        updateContactInfo(dto, trainer);
        updateOfficeInfo(dto, trainer);
        updateProfessionalInfo(dto, trainer);
        updateFinancialInfo(dto, trainer);
        updateMiscellaneousInfo(dto, trainer);
        trainer.setUpdatedAt(now);
    }

    private void updateBasicInfo(TrainerUpdateRequestDTO dto, Trainer trainer) {
        if (dto.getDesignation() != null && !dto.getDesignation().trim().isEmpty())
            trainer.setDesignation(dto.getDesignation().trim());
    }

    private void updateContactInfo(TrainerUpdateRequestDTO dto, Trainer trainer) {
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().trim().isEmpty())
            trainer.setPhoneNumber(dto.getPhoneNumber().trim());
    }

    private void updateOfficeInfo(TrainerUpdateRequestDTO dto, Trainer trainer) {
        if (dto.getOfficeName() != null && !dto.getOfficeName().trim().isEmpty())
            trainer.setOfficeName(dto.getOfficeName().trim());
        if (dto.getOfficeAddress() != null && !dto.getOfficeAddress().trim().isEmpty())
            trainer.setOfficeAddress(dto.getOfficeAddress().trim());
        if (dto.getLinkedInOrWebsiteURL() != null && !dto.getLinkedInOrWebsiteURL().trim().isEmpty())
            trainer.setLinkedInOrWebsiteURL(dto.getLinkedInOrWebsiteURL().trim());
    }

    private void updateProfessionalInfo(TrainerUpdateRequestDTO dto, Trainer trainer) {
        if (dto.getTrainingSpecialization() != null && !dto.getTrainingSpecialization().trim().isEmpty())
            trainer.setTrainingSpecialization(dto.getTrainingSpecialization().trim());
        if (dto.getYearsOfExperience() != null)
            trainer.setYearsOfExperience(dto.getYearsOfExperience());
        if (dto.getQualification() != null && !dto.getQualification().trim().isEmpty())
            trainer.setQualification(dto.getQualification().trim());
        if (dto.getModesOfTrainingPreferred() != null && !dto.getModesOfTrainingPreferred().isEmpty())
            trainer.setModesOfTrainingPreferred(dto.getModesOfTrainingPreferred());
        if (dto.getClientsTrainedBefore() != null && !dto.getClientsTrainedBefore().isEmpty())
            trainer.setClientsTrainedBefore(dto.getClientsTrainedBefore());
    }

    private void updateFinancialInfo(TrainerUpdateRequestDTO dto, Trainer trainer) {
        if (dto.getPanNumber() != null && !dto.getPanNumber().trim().isEmpty())
            trainer.setPanNumber(EncryptionUtil.encrypt(dto.getPanNumber().trim()));
        if (dto.getBankName() != null && !dto.getBankName().trim().isEmpty())
            trainer.setBankName(dto.getBankName().trim());
        if (dto.getBranchName() != null && !dto.getBranchName().trim().isEmpty())
            trainer.setBranchName(dto.getBranchName().trim());
        if (dto.getAccountNumber() != null && !dto.getAccountNumber().trim().isEmpty())
            trainer.setAccountNumber(EncryptionUtil.encrypt(dto.getAccountNumber().trim()));
        if (dto.getIfscCode() != null && !dto.getIfscCode().trim().isEmpty())
            trainer.setIfscCode(EncryptionUtil.encrypt(dto.getIfscCode().trim()));
    }

    @Override
    public List<TrainerAdminResponse> getAllTrainersForAdmin() {
        try {
            List<Trainer> trainers = repository.findAll();
            if (trainers == null || trainers.isEmpty()) return new ArrayList<>();

            List<TrainerAdminResponse> responseList = new ArrayList<>();
            for (Trainer trainer : trainers) {
                if (trainer != null) {
                    responseList.add(mapEntityToAdminResponse(trainer));
                }
            }
            return responseList;
        } catch (Exception ex) {
            log.error("Failed to fetch all trainers for admin. Reason: {}", ex.getMessage(), ex);
            throw new UserServiceException("Failed to fetch trainers for admin. Please try again.");
        }
    }

    private void updateMiscellaneousInfo(TrainerUpdateRequestDTO dto, Trainer trainer) {
        if (dto.getGlobalCertifications() != null && !dto.getGlobalCertifications().trim().isEmpty())
            trainer.setGlobalCertifications(dto.getGlobalCertifications().trim());
        if (dto.getTopRegistration() != null && !dto.getTopRegistration().trim().isEmpty())
            trainer.setTopRegistration(dto.getTopRegistration().trim());
        if (dto.getSupportingDocumentsChecklist() != null && !dto.getSupportingDocumentsChecklist().isEmpty())
            trainer.setSupportingDocumentsChecklist(dto.getSupportingDocumentsChecklist());
        if (dto.getAnyLegalDisputesInPast5Years() != null)
            trainer.setAnyLegalDisputesInPast5Years(dto.getAnyLegalDisputesInPast5Years());
    }
}