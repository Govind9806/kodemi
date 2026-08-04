package com.example.user_service.service;

import com.example.user_service.exception.TrainerNotFoundException;
import com.example.user_service.model.Rating;
import com.example.user_service.model.Trainer;
import com.example.user_service.repository.RatingRepository;
import com.example.user_service.repository.TrainerRepository;
import com.example.user_service.service.impl.RatingServiceImpl;
import com.example.user_service.util.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class RatingServiceImplTest {

    @Mock
    private TrainerRepository trainerRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RatingServiceImpl ratingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRateTrainerSuccess() {
        String token = "token";
        String userId = "user1";
        String trainerId = "trainer1";

        when(jwtUtil.extractUserId(token)).thenReturn(userId);

        Trainer trainer = new Trainer();
        trainer.setUserId(trainerId);

        when(trainerRepository.findById(trainerId)).thenReturn(trainer);

        List<Rating> ratings = List.of(
                new Rating("u1", trainerId, 4.0f),
                new Rating("u2", trainerId, 5.0f)
        );

        when(ratingRepository.findByTrainerId(trainerId)).thenReturn(ratings);

        String result = ratingService.rateTrainer(token, trainerId, 5.0f);

        assertEquals("Trainer Rated Successfully", result);
        verify(jwtUtil).extractUserId(token);
        verify(ratingRepository).save(any(Rating.class));
        verify(trainerRepository).findById(trainerId);
        verify(ratingRepository).findByTrainerId(trainerId);
        verify(trainerRepository).save(trainer);
        assertEquals(4.5f, trainer.getRatingValue());
    }

    @Test
    void testTrainerNotFound() {
        String token = "token";
        String trainerId = "invalid";

        when(jwtUtil.extractUserId(token)).thenReturn("user1");
        when(trainerRepository.findById(trainerId)).thenReturn(null);

        assertThrows(TrainerNotFoundException.class,
                () -> ratingService.rateTrainer(token, trainerId, 4.0f));

        // Rating should NOT be saved since trainer doesn't exist
        verify(ratingRepository, never()).save(any());
        verify(trainerRepository).findById(trainerId);
        verify(trainerRepository, never()).save(any());
    }

    @Test
    void testAverageMultipleRatings() {
        String token = "token";
        String trainerId = "trainer1";

        when(jwtUtil.extractUserId(token)).thenReturn("user1");

        Trainer trainer = new Trainer();
        when(trainerRepository.findById(trainerId)).thenReturn(trainer);

        List<Rating> ratings = List.of(
                new Rating("u1", trainerId, 2.0f),
                new Rating("u2", trainerId, 4.0f),
                new Rating("u3", trainerId, 6.0f)
        );

        when(ratingRepository.findByTrainerId(trainerId)).thenReturn(ratings);

        ratingService.rateTrainer(token, trainerId, 5.0f);

        assertEquals(4.0f, trainer.getRatingValue());
    }

    @Test
    void testSingleRatingAverage() {
        String token = "token";
        String trainerId = "trainer1";

        when(jwtUtil.extractUserId(token)).thenReturn("user1");

        Trainer trainer = new Trainer();
        when(trainerRepository.findById(trainerId)).thenReturn(trainer);

        when(ratingRepository.findByTrainerId(trainerId))
                .thenReturn(List.of(new Rating("user1", trainerId, 3.0f)));

        ratingService.rateTrainer(token, trainerId, 3.0f);

        assertEquals(3.0f, trainer.getRatingValue());
    }

    @Test
    void testNoRatingsAverageZero() {
        String token = "token";
        String trainerId = "trainer1";

        when(jwtUtil.extractUserId(token)).thenReturn("user1");

        Trainer trainer = new Trainer();
        when(trainerRepository.findById(trainerId)).thenReturn(trainer);

        when(ratingRepository.findByTrainerId(trainerId))
                .thenReturn(List.of());

        ratingService.rateTrainer(token, trainerId, 5.0f);

        assertEquals(0.0f, trainer.getRatingValue());
    }

    @Test
    void testSavedRatingFields() {
        String token = "token";
        String userId = "userX";
        String trainerId = "trainerX";

        when(jwtUtil.extractUserId(token)).thenReturn(userId);

        Trainer trainer = new Trainer();
        when(trainerRepository.findById(trainerId)).thenReturn(trainer);

        when(ratingRepository.findByTrainerId(trainerId))
                .thenReturn(List.of(new Rating(userId, trainerId, 5.0f)));

        ArgumentCaptor<Rating> captor = ArgumentCaptor.forClass(Rating.class);

        ratingService.rateTrainer(token, trainerId, 5.0f);

        verify(ratingRepository).save(captor.capture());

        Rating saved = captor.getValue();

        assertEquals(userId, saved.getUserId());
        assertEquals(trainerId, saved.getTrainerId());
        assertEquals(5.0f, saved.getRatingValue());
    }

    @Test
    void testRateTrainer_invalidRating_throwsIllegalArgumentException() {
        when(jwtUtil.extractUserId("validToken")).thenReturn("usr1");
        when(trainerRepository.findById("t1")).thenReturn(new Trainer());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ratingService.rateTrainer("validToken", "t1", 6.0f));
    }

    @Test
    void testRateTrainer_trainerNotFound_throwsTrainerNotFoundException() {
        when(jwtUtil.extractUserId("validToken")).thenReturn("usr1");
        when(trainerRepository.findById("t_missing")).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(TrainerNotFoundException.class,
                () -> ratingService.rateTrainer("validToken", "t_missing", 4.0f));
    }
}