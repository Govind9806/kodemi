package com.example.user_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.example.user_service.model.Learner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnerRepositoryTest {

    @Mock
    private DynamoDBMapper dynamoDBMapper;

    @InjectMocks
    private LearnerRepository learnerRepository;

    @Test
    void testSave_callsDynamoDBMapperSave() {
        Learner learner = new Learner();
        learner.setUserId("u123");

        learnerRepository.save(learner);

        verify(dynamoDBMapper, times(1)).save(learner);
    }

    @Test
    void testFindByUserId_returnsLearner() {
        String userId = "u123";

        Learner learner = new Learner();
        learner.setUserId(userId);

        when(dynamoDBMapper.load(Learner.class, userId))
                .thenReturn(learner);

        Learner result = learnerRepository.findByUserId(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(dynamoDBMapper).load(Learner.class, userId);
    }

    @Test
    void testFindAll_returnsListOfLearners() {
        PaginatedScanList<Learner> mockScanList = mock(PaginatedScanList.class);

        when(dynamoDBMapper.scan(eq(Learner.class), any(DynamoDBScanExpression.class)))
                .thenReturn(mockScanList);

        when(mockScanList.size()).thenReturn(2);

        List<Learner> result = learnerRepository.findAll();

        assertEquals(2, result.size());
        verify(dynamoDBMapper).scan(eq(Learner.class), any(DynamoDBScanExpression.class));
    }

    @Test
    void testDelete_existingLearner_callsDelete() {
        String userId = "u123";

        Learner learner = new Learner();
        learner.setUserId(userId);

        when(dynamoDBMapper.load(Learner.class, userId))
                .thenReturn(learner);

        learnerRepository.delete(userId);

        verify(dynamoDBMapper).load(Learner.class, userId);
        verify(dynamoDBMapper).delete(learner);
    }

    @Test
    void testDelete_nonExistingLearner_doesNotCallDelete() {
        String userId = "u123";

        when(dynamoDBMapper.load(Learner.class, userId))
                .thenReturn(null);

        learnerRepository.delete(userId);

        verify(dynamoDBMapper).load(Learner.class, userId);
        verify(dynamoDBMapper, never()).delete(any());
    }

    @Test
    void testGetLearnerByEmail_returnsLearner() {
        String email = "test@example.com";

        Learner learner = new Learner();
        learner.setEmail(email);

        PaginatedQueryList<Learner> mockQueryList = mock(PaginatedQueryList.class);

        when(dynamoDBMapper.query(eq(Learner.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(mockQueryList);

        when(mockQueryList.isEmpty()).thenReturn(false);
        when(mockQueryList.get(0)).thenReturn(learner);

        Learner result = learnerRepository.getLearnerByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(dynamoDBMapper).query(eq(Learner.class), any(DynamoDBQueryExpression.class));
    }

    @Test
    void testGetLearnerByEmail_returnsNullWhenEmpty() {
        PaginatedQueryList<Learner> mockQueryList = mock(PaginatedQueryList.class);

        when(dynamoDBMapper.query(eq(Learner.class), any(DynamoDBQueryExpression.class)))
                .thenReturn(mockQueryList);

        when(mockQueryList.isEmpty()).thenReturn(true);

        Learner result = learnerRepository.getLearnerByEmail("notfound@example.com");

        assertNull(result);
        verify(dynamoDBMapper).query(eq(Learner.class), any(DynamoDBQueryExpression.class));
    }
}
