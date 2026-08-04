package com.example.user_service.service.impl;

import com.example.user_service.exception.TrainerNotFoundException;
import com.example.user_service.model.Rating;
import com.example.user_service.model.Trainer;
import com.example.user_service.repository.RatingRepository;
import com.example.user_service.repository.TrainerRepository;
import com.example.user_service.service.RatingService;
import com.example.user_service.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RatingServiceImpl implements RatingService {

    private final TrainerRepository trainerRepository;
    private final RatingRepository ratingRepository;
    private final JwtUtil jwtUtil;

    public RatingServiceImpl(TrainerRepository trainerRepository, RatingRepository ratingRepository, JwtUtil jwtUtil) {
        this.trainerRepository = trainerRepository;
        this.ratingRepository = ratingRepository;
        this.jwtUtil = jwtUtil;
    }

    public String rateTrainer(String token, String trainerId, Float rating) {
        String userId = jwtUtil.extractUserId(token);
        log.info("Request to rate trainer received | userId: {}, trainerId: {}, rating: {}", userId, trainerId, rating);

        // Validate trainer exists BEFORE saving the rating
        Trainer trainer = trainerRepository.findById(trainerId);
        if (trainer == null) {
            log.warn("Trainer validation failed - trainer not found with id: {}", trainerId);
            throw new TrainerNotFoundException("Trainer not found with id: " + trainerId);
        }

        // Validate rating value
        if (rating == null || rating < 0 || rating > 5) {
            log.warn("Rating validation failed - invalid value: {}", rating);
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }

        Rating newRating = new Rating();
        newRating.setRatingValue(rating);
        newRating.setTrainerId(trainerId);
        newRating.setUserId(userId);
        ratingRepository.save(newRating);
        log.info("Saved new rating record to DynamoDB for trainerId: {}", trainerId);

        // Recalculate average rating atomically
        updateTrainerAverageRating(trainer, trainerId);
        
        log.info("Trainer {} rated successfully by user {}", trainerId, userId);
        return "Trainer Rated Successfully";
    }


    private void updateTrainerAverageRating(Trainer trainer, String trainerId) {
        log.info("Recalculating average rating for trainerId: {}", trainerId);

        List<Rating> ratings = ratingRepository.findByTrainerId(trainerId);

        log.info("Fetched {} total ratings for trainerId: {}",
                ratings != null ? ratings.size() : 0, trainerId);

        if (ratings == null || ratings.isEmpty()) {
            trainer.setRatingValue(0.0F);
        } else {
            float sum = (float) ratings.stream()
                    .mapToDouble(Rating::getRatingValue)
                    .sum();

            trainer.setRatingValue(sum / ratings.size());
        }

        log.info("Calculated new average rating for trainerId: {} | average: {}",
                trainerId, trainer.getRatingValue());

        trainerRepository.save(trainer);

        log.info("Saved updated trainer average rating back to database");
    }
}
