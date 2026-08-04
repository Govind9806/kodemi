package com.example.user_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.user_service.model.Learner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LearnerRepository {

    private final DynamoDBMapper dynamoDBMapper;
    private static final int MAX_SCAN_LIMIT = 100;

    public void save(Learner userProfile) {
        dynamoDBMapper.save(userProfile);
    }

    public Learner findByUserId(String userId) {
        return dynamoDBMapper.load(Learner.class, userId);
    }

    /**
     * Scans learners with pagination limit to prevent performance issues.
     * For large datasets, consider implementing pagination with ExclusiveStartKey.
     */
    public List<Learner> findAll() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withLimit(MAX_SCAN_LIMIT);
        
        List<Learner> learners = dynamoDBMapper.scan(Learner.class, scanExpression);
        log.debug("Scanned {} learners from DynamoDB", learners.size());
        return learners;
    }

    public void delete(String userId) {
        Learner learner = dynamoDBMapper.load(Learner.class, userId);
        if (learner != null) {
            dynamoDBMapper.delete(learner);
        }
    }
    
    public Learner getLearnerByEmail(String email) {

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":email", new AttributeValue().withS(email));

        DynamoDBQueryExpression<Learner> query =
                new DynamoDBQueryExpression<Learner>()
                        .withIndexName("email-index")
                        .withConsistentRead(false)
                        .withKeyConditionExpression("email = :email")
                        .withExpressionAttributeValues(values);

        List<Learner> users = dynamoDBMapper.query(Learner.class, query);
        return users.isEmpty() ? null : users.get(0);
    }
}

