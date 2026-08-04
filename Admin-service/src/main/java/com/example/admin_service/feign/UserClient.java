package com.example.admin_service.feign;

import com.example.admin_service.dto.response.AdminResponseDTO;
import com.example.admin_service.dto.response.LearnerResponseDTO;
import com.example.admin_service.dto.response.TrainerResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;

@FeignClient(name = "user-service", fallbackFactory = com.example.admin_service.feign.fallback.UserClientFallbackFactory.class)
@Retry(name = "default")
public interface UserClient {
    @GetMapping("api/v1/trainer/details/{id}")
    TrainerResponseDTO getTrainerById(@RequestHeader("Authorization") String token,@PathVariable("id") String trainerId);
    
    @GetMapping("api/v1/trainer/detail/all")
    List<TrainerResponseDTO> getAllTrainers(@RequestHeader("Authorization") String token);

    @GetMapping("api/v1/user/admin/details/{id}")
    AdminResponseDTO getAdmin(@PathVariable("id") String id);

    @GetMapping("api/v1/trainer/all/pending")
    List<TrainerResponseDTO> getAllPendingTrainers(@RequestHeader("Authorization")  String token);

    @GetMapping("api/v1/learner/details/{id}")
    LearnerResponseDTO getLearner(@RequestHeader("Authorization") String token, @PathVariable("id") String id);
}
