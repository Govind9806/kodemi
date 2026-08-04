package com.example.user_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.example.user_service.model.Rating;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RatingRepository {
    private final DynamoDBMapper dynamoDBMapper;

    public RatingRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(Rating rating) {
        dynamoDBMapper.save(rating);
    }
    public List<Rating> findByTrainerId(String trainerId) {
        Rating ratingKey = new Rating();
        ratingKey.setTrainerId(trainerId);

        DynamoDBQueryExpression<Rating> queryExpression = new DynamoDBQueryExpression<Rating>()
                .withIndexName("trainerId-index") // Use the GSI
                .withConsistentRead(false)        // GSI must use eventually consistent read
                .withHashKeyValues(ratingKey);

        return dynamoDBMapper.query(Rating.class, queryExpression);
    }

}
