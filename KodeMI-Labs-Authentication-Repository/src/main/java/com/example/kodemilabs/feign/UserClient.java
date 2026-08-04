package com.example.kodemilabs.feign;

import com.example.kodemilabs.dto.request.TrainerRequestDTO;
import com.example.kodemilabs.dto.request.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    @PostMapping(value = "/api/v1/learner/create", consumes = "application/json")
    void addLearner(@RequestBody UserDTO userDTO);

    @PostMapping(value = "/api/v1/trainer/apply", consumes = "application/json")
    void createTrainer(@RequestBody TrainerRequestDTO trainerRequestDTO);
}
