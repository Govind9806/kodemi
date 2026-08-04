package com.example.admin_service.feign.fallback;

import com.example.admin_service.feign.AuthClient;
import com.example.admin_service.dto.response.AdminLoginRequest;
import com.example.admin_service.dto.request.UserDTO;
import com.example.admin_service.dto.request.TrainerReviewRequest;
import com.example.admin_service.exceptions.DownstreamServiceException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthClientFallbackFactory implements FallbackFactory<AuthClient> {
    @Override
    public AuthClient create(Throwable cause) {
        return new AuthClient() {
            @Override
            public ResponseEntity<Object> reviewTrainer(String token, String userId, TrainerReviewRequest request) {
                log.error("AuthClient reviewTrainer failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Auth service is currently unavailable.", cause);
            }

            @Override
            public ResponseEntity<Object> checkStatus(String email) {
                log.error("AuthClient checkStatus failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Auth service is currently unavailable.", cause);
            }

            @Override
            public Object adminLogin(AdminLoginRequest adminResponseDTO) {
                log.error("AuthClient adminLogin failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Auth service is currently unavailable.", cause);
            }

            @Override
            public UserDTO geUserById(String token, String userId) {
                log.error("AuthClient geUserById failed: {}", cause.getMessage(), cause);
                throw new DownstreamServiceException("Auth service is currently unavailable.", cause);
            }
        };
    }
}
