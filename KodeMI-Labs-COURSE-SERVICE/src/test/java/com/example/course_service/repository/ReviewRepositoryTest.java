package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.model.ReviewEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewRepositoryTest {

    private DynamoDBMapper dynamoDBMapper;
    private ReviewRepository reviewRepository;

    @BeforeEach
    void setup() {
        dynamoDBMapper = mock(DynamoDBMapper.class);
        reviewRepository = new ReviewRepository(dynamoDBMapper);
    }

    @Test
    void save_CallsDynamoDBMapper() {
        ReviewEntity review = new ReviewEntity();
        review.setReviewId("R101");

        reviewRepository.save(review);

        verify(dynamoDBMapper, times(1)).save(review);
    }

    @Test
    void findById_ReturnsReview() {
        ReviewEntity review = new ReviewEntity();
        review.setReviewId("R101");

        when(dynamoDBMapper.load(ReviewEntity.class, "R101")).thenReturn(review);

        ReviewEntity result = reviewRepository.findById("R101");

        verify(dynamoDBMapper, times(1)).load(ReviewEntity.class, "R101");
        assertEquals("R101", result.getReviewId());
    }

    @Test
    void findById_ReturnsNull_WhenNotFound() {
        when(dynamoDBMapper.load(ReviewEntity.class, "999")).thenReturn(null);

        assertNull(reviewRepository.findById("999"));
    }

    @Test
    void findAll_ReturnsList() {
        PaginatedScanList<ReviewEntity> paginatedList = mock(PaginatedScanList.class);
        when(paginatedList.size()).thenReturn(2);

        ReviewEntity r1 = new ReviewEntity();
        r1.setReviewId("R101");
        ReviewEntity r2 = new ReviewEntity();
        r2.setReviewId("R102");
        when(paginatedList.get(0)).thenReturn(r1);
        when(paginatedList.get(1)).thenReturn(r2);

        when(dynamoDBMapper.scan(eq(ReviewEntity.class), any(DynamoDBScanExpression.class)))
                .thenReturn(paginatedList);

        List<ReviewEntity> list = reviewRepository.findAll();

        verify(dynamoDBMapper, times(1))
                .scan(eq(ReviewEntity.class), any(DynamoDBScanExpression.class));
        assertEquals(2, list.size());
    }

    @Test
    void findByCourseId_ReturnsList() {
        PaginatedQueryList<ReviewEntity> queryResult = mock(PaginatedQueryList.class);
        when(dynamoDBMapper.query(eq(ReviewEntity.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(queryResult);

        List<ReviewEntity> result = reviewRepository.findByCourseId("C101");

        verify(dynamoDBMapper, times(1))
                .query(eq(ReviewEntity.class), any(DynamoDBQueryExpression.class));
        assertNotNull(result);
    }
}