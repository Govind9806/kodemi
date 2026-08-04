package com.example.kodemilabs.feign;

import com.example.kodemilabs.dto.request.TrainerRequestDTO;
import com.example.kodemilabs.dto.request.UserDTO;
import com.example.kodemilabs.exceptions.DownstreamServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public void addLearner(UserDTO userDTO) {
                log.error("UserClient.addLearner failed. Downstream service unavailable. Cause: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Downstream user-service is currently unavailable.", cause);
            }

            @Override
            public void createTrainer(TrainerRequestDTO trainerRequestDTO) {
                log.error("UserClient.createTrainer failed. Downstream service unavailable. Cause: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Downstream user-service is currently unavailable.", cause);
            }
        };
    }
}
