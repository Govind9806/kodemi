package com.example.user_service.feign;

import com.example.user_service.dto.response.TrainerResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "admin-service", fallbackFactory = com.example.user_service.feign.fallback.AdminClientFallbackFactory.class)
public interface AdminClient {

    @PostMapping("/api/v1/admin/trainer/new-application")
    void notifyNewTrainerApplication(@RequestBody TrainerResponseDTO trainerDetails);
}