package com.example.user_service.service;

import com.example.user_service.dto.request.TrainerRequestDTO;
import com.example.user_service.dto.response.TrainerAdminResponse;
import com.example.user_service.dto.response.TrainerResponseDTO;
import com.example.user_service.dto.update.TrainerUpdateRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.example.user_service.dto.response.TrainerUserResponseDTO;

public interface TrainerService {

    String createTrainerProfile(
            TrainerRequestDTO request,
            com.example.user_service.dto.request.TrainerProfileFiles files,
            String userId,
            String token);

    TrainerResponseDTO getTrainerProfileById(String userId);

    String updateTrainerProfile(
            String userId,
            TrainerUpdateRequestDTO request,
            MultipartFile file);

    String deleteTrainerProfile(String userId);

    List<TrainerResponseDTO> getAllTrainers();
    List<TrainerResponseDTO> getAllPendingTrainers();
    List<TrainerUserResponseDTO> getAllTrainersForUsers();
    List<TrainerAdminResponse> getAllTrainersForAdmin();
}

