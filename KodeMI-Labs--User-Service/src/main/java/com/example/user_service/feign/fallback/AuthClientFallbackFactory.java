package com.example.user_service.feign.fallback;

import com.example.user_service.commondto.UserDTO;
import com.example.user_service.exception.DownstreamServiceException;
import com.example.user_service.feign.AuthClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AuthClientFallbackFactory implements FallbackFactory<AuthClient> {
    private static final String AUTH_SERVICE_FAILED ="Auth service is currently unavailable. Please try again later.";
    @Override
    public AuthClient create(Throwable cause) {
        return new AuthClient() {
            @Override
            public UserDTO getUserByEmail(String email) {
                log.error("Critical Auth service call failed for getUserByEmail. Error: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException(AUTH_SERVICE_FAILED, cause);
            }

            @Override
            public String updateUserRole(String userId, String role, String token) {
                log.error("Critical Auth service call failed for updateUserRole. Error: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException(AUTH_SERVICE_FAILED, cause);
            }

            @Override
            public void markTrainerSubmitted(String userId, String token) {
                log.error("Critical Auth service call failed for markTrainerSubmitted. Error: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException(AUTH_SERVICE_FAILED, cause);
            }

            @Override
            public List<String> getPendingTrainers() {
                log.error("Critical Auth service call failed for getPendingTrainers. Error: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException(AUTH_SERVICE_FAILED, cause);
            }

            @Override
            public List<UserDTO> getUsersByIds(List<String> userIds) {
                log.error("Critical Auth service call failed for getUsersByIds. Error: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException(AUTH_SERVICE_FAILED, cause);
            }
        };
    }
}
