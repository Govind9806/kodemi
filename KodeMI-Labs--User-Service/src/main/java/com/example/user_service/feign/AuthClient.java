package com.example.user_service.feign;

import com.example.user_service.commondto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name = "auth-service", fallbackFactory = com.example.user_service.feign.fallback.AuthClientFallbackFactory.class)
public interface AuthClient {
    @GetMapping("/api/v1/auth/user/{email}")
    UserDTO getUserByEmail(@PathVariable String email);

    @PutMapping("/api/v1/auth/role/{userId}")
    String updateUserRole(
            @PathVariable("userId") String userId,
            @RequestParam("role") String role,
            @RequestHeader("Authorization") String token
    );
    @PostMapping("/api/v1/auth/internal/trainer/submitted/{userId}")
    void markTrainerSubmitted(@PathVariable("userId") String userId, @RequestHeader("Authorization") String token);
    @GetMapping("api/v1/auth/pending/trainer")
    List<String> getPendingTrainers();

    @PostMapping("/api/v1/auth/internal/users/batch")
    List<UserDTO> getUsersByIds(@RequestBody List<String> userIds);
}



