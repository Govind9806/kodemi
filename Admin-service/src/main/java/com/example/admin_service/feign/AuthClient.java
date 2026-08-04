package com.example.admin_service.feign;

import com.example.admin_service.dto.response.AdminLoginRequest;
import com.example.admin_service.dto.request.UserDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "auth-service", fallbackFactory = com.example.admin_service.feign.fallback.AuthClientFallbackFactory.class)
@Retry(name = "default")
public interface AuthClient {
	
    @PutMapping("api/v1/auth/internal/trainer/review/{userId}")
    ResponseEntity<Object> reviewTrainer(
            @RequestHeader("Authorization") String token, 
            @PathVariable("userId") String userId, 
            @RequestBody com.example.admin_service.dto.request.TrainerReviewRequest request);

    @GetMapping("api/v1/auth/status")
    ResponseEntity<Object> checkStatus(
            @RequestParam @Email @NotBlank String email);
            
    @PostMapping("api/v1/auth/admin/login")
    Object adminLogin(@RequestBody AdminLoginRequest adminResponseDTO);

    @GetMapping("api/v1/auth/user/id/{userId}")
    UserDTO geUserById(@RequestHeader("Authorization") String token, @PathVariable("userId") String userId);
}
