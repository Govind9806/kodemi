package com.example.user_service.controller;

import com.example.user_service.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rating")
public class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/rate")
    public ResponseEntity<String> rateTrainer(@RequestHeader("Authorization") String token,
                                              @RequestParam String trainerId,
                                              @RequestParam Float rating){
        return  ResponseEntity.ok(ratingService.rateTrainer(token, trainerId, rating));
    }
}
