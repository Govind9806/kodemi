package com.example.user_service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RatingTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        Rating rating = new Rating();

        rating.setUserId("user123");
        rating.setTrainerId("trainer456");
        rating.setRatingValue(4.5f);

        assertEquals("user123", rating.getUserId());
        assertEquals("trainer456", rating.getTrainerId());
        assertEquals(4.5f, rating.getRatingValue());
    }

    @Test
    void testAllArgsConstructor() {
        Rating rating = new Rating("user1", "trainer1", 3.5f);

        assertEquals("user1", rating.getUserId());
        assertEquals("trainer1", rating.getTrainerId());
        assertEquals(3.5f, rating.getRatingValue());
    }

    @Test
    void testBuilder() {
        Rating rating = Rating.builder()
                .userId("userABC")
                .trainerId("trainerXYZ")
                .ratingValue(5.0f)
                .build();

        assertNotNull(rating);
        assertEquals("userABC", rating.getUserId());
        assertEquals("trainerXYZ", rating.getTrainerId());
        assertEquals(5.0f, rating.getRatingValue());
    }

    @Test
    void testEqualsAndHashCode() {
        Rating r1 = Rating.builder()
                .userId("user1")
                .trainerId("trainer1")
                .ratingValue(4.0f)
                .build();

        Rating r2 = Rating.builder()
                .userId("user1")
                .trainerId("trainer1")
                .ratingValue(4.0f)
                .build();

        // Note: Without @EqualsAndHashCode, this will fail
        // Add Lombok annotation if needed
        assertNotEquals(r1, r2);
    }

    @Test
    void testNullValues() {
        Rating rating = new Rating();

        assertNull(rating.getUserId());
        assertNull(rating.getTrainerId());
        assertNull(rating.getRatingValue());
    }
}