package com.example.user_service.service;

import com.example.user_service.commondto.UserDTO;
import com.example.user_service.dto.request.LearnerRequestDTO;
import com.example.user_service.dto.response.LearnerResponseDTO;
import com.example.user_service.dto.response.UserNotificationTargetDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LearnerService {
    
    LearnerResponseDTO getProfileByUserId(String userId);
    
    List<LearnerResponseDTO> getAllProfiles();
    
    LearnerResponseDTO updateProfile(String userId, LearnerRequestDTO requestDTO, MultipartFile file);
    
    void deleteProfile(String userId);

    void createLearner(UserDTO userDTO);
    List<UserNotificationTargetDTO> getLearnerNotificationTargets();

    void followTrainer(String userId, String trainerId);
    void unfollowTrainer(String userId, String trainerId);
    List<String> getFollowedTrainers(String userId);
}
