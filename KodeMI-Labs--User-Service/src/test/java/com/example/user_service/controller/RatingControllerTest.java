package com.example.user_service.controller;

import com.example.user_service.service.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RatingControllerTest {

    @InjectMocks
    private RatingController ratingController;

    @Mock
    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void rateTrainer_success() {
        String token = "Bearer token";
        String trainerId = "trainer1";
        Float rating = 4.5f;

        when(ratingService.rateTrainer(token, trainerId, rating))
                .thenReturn("Trainer Rated Successfully");

        ResponseEntity<String> response = ratingController.rateTrainer(token, trainerId, rating);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Trainer Rated Successfully", response.getBody());
        verify(ratingService).rateTrainer(token, trainerId, rating);
    }

    @Test
    void rateTrainer_differentRatingValues() {
        String token = "Bearer token";
        String trainerId = "trainer2";

        when(ratingService.rateTrainer(token, trainerId, 1.0f)).thenReturn("Trainer Rated Successfully");
        when(ratingService.rateTrainer(token, trainerId, 5.0f)).thenReturn("Trainer Rated Successfully");

        assertEquals("Trainer Rated Successfully",
                ratingController.rateTrainer(token, trainerId, 1.0f).getBody());
        assertEquals("Trainer Rated Successfully",
                ratingController.rateTrainer(token, trainerId, 5.0f).getBody());
    }

    @Test
    void rateTrainer_serviceThrows_propagatesException() {
        String token = "Bearer token";
        String trainerId = "invalid";

        when(ratingService.rateTrainer(token, trainerId, 3.0f))
                .thenThrow(new RuntimeException("Trainer not found"));

        assertThrows(RuntimeException.class,
                () -> ratingController.rateTrainer(token, trainerId, 3.0f));
    }
}
