package com.example.user_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.example.user_service.model.Rating;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class RatingRepositoryTest {

    @Mock
    private DynamoDBMapper dynamoDBMapper;

    @InjectMocks
    private RatingRepository ratingRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ===== save =====
    @Test
    void save_callsDynamoDBMapper() {
        Rating rating = new Rating();

        ratingRepository.save(rating);

        verify(dynamoDBMapper, times(1)).save(rating);
    }

    // ===== findByTrainerId =====
    @Test
    void findByTrainerId_returnsList() {
        String trainerId = "trainer123";

        PaginatedQueryList<Rating> mockList = mock(PaginatedQueryList.class);

        when(dynamoDBMapper.query(eq(Rating.class), any())).thenReturn(mockList);

        List<Rating> result = ratingRepository.findByTrainerId(trainerId);

        assertEquals(mockList, result);
        verify(dynamoDBMapper, times(1))
                .query(eq(Rating.class), any());
    }

    // ===== findByTrainerId_emptyResult =====
    @Test
    void findByTrainerId_returnsEmptyList() {
        String trainerId = "trainer123";

        PaginatedQueryList<Rating> mockList = mock(PaginatedQueryList.class);
        when(mockList.isEmpty()).thenReturn(true);

        when(dynamoDBMapper.query(eq(Rating.class), any())).thenReturn(mockList);

        List<Rating> result = ratingRepository.findByTrainerId(trainerId);

        assertTrue(result.isEmpty());
    }

    // ===== findByTrainerId_nullTrainerId =====
    @Test
    void findByTrainerId_nullTrainerId() {
        PaginatedQueryList<Rating> mockList = mock(PaginatedQueryList.class);

        when(dynamoDBMapper.query(eq(Rating.class), any())).thenReturn(mockList);

        List<Rating> result = ratingRepository.findByTrainerId(null);

        assertNotNull(result);
        verify(dynamoDBMapper).query(eq(Rating.class), any());
    }
}