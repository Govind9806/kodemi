package com.example.admin_service.feign.fallback;

import com.example.admin_service.feign.UserClient;
import com.example.admin_service.dto.response.AdminResponseDTO;
import com.example.admin_service.dto.response.LearnerResponseDTO;
import com.example.admin_service.dto.response.TrainerResponseDTO;
import com.example.admin_service.exceptions.DownstreamServiceException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public TrainerResponseDTO getTrainerById(String token, String trainerId) {
                log.error("UserClient getTrainerById failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("User service is currently unavailable.", cause);
            }

            @Override
            public List<TrainerResponseDTO> getAllTrainers(String token) {
                log.error("UserClient getAllTrainers failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("User service is currently unavailable.", cause);
            }

            @Override
            public AdminResponseDTO getAdmin(String id) {
                log.error("UserClient getAdmin failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("User service is currently unavailable.", cause);
            }

            @Override
            public List<TrainerResponseDTO> getAllPendingTrainers(String token) {
                log.error("UserClient getAllPendingTrainers failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("User service is currently unavailable.", cause);
            }

            @Override
            public LearnerResponseDTO getLearner(String token, String id) {
                log.error("UserClient getLearner failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("User service is currently unavailable.", cause);
            }
        };
    }
}
