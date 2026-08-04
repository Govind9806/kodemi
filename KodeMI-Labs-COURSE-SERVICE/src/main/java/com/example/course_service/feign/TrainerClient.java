package com.example.course_service.feign;

import com.example.course_service.dto.response.TrainerResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", fallbackFactory = TrainerClientFallbackFactory.class)
public interface TrainerClient {

    @GetMapping("api/v1/trainer/detail")
    TrainerResponseDTO getTrainer(@RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/trainer/details/{userId}")
    TrainerResponseDTO getTrainerById(@PathVariable("userId") String userId);
}
