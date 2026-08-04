package com.example.course_service.feign;

import com.example.course_service.dto.response.TrainerResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class TrainerClientFallbackFactory implements FallbackFactory<TrainerClient> {

    private static final Logger log = LoggerFactory.getLogger(TrainerClientFallbackFactory.class);

    @Override
    public TrainerClient create(Throwable cause) {
        return new TrainerClient() {
            @Override
            public TrainerResponseDTO getTrainer(String token) {
                log.warn("TrainerClient.getTrainer failed. Falling back to default generic Trainer. Cause: {}", cause.getMessage(), cause);
                return TrainerResponseDTO.builder()
                        .fullName("Trainer")
                        .profilePictureURL("default.png")
                        .build();
            }

            @Override
            public TrainerResponseDTO getTrainerById(String userId) {
                log.warn("TrainerClient.getTrainerById failed for userId: {}. Falling back to default generic Trainer. Cause: {}", userId, cause.getMessage(), cause);
                return TrainerResponseDTO.builder()
                        .userId(userId)
                        .fullName("Trainer")
                        .profilePictureURL("default.png")
                        .build();
            }
        };
    }
}
