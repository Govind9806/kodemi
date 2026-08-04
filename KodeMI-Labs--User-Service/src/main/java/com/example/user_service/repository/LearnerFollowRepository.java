package com.example.user_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.example.user_service.model.LearnerFollow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LearnerFollowRepository {


    private final  DynamoDBMapper dynamoDBMapper;

    public LearnerFollowRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void follow(LearnerFollow follow) {
        dynamoDBMapper.save(follow);
    }

    public void unfollow(String userId, String trainerId) {
        LearnerFollow follow = dynamoDBMapper.load(LearnerFollow.class, userId, trainerId);
        if (follow != null) {
            dynamoDBMapper.delete(follow);
        }
    }

    public boolean isFollowing(String userId, String trainerId) {
        LearnerFollow follow = dynamoDBMapper.load(LearnerFollow.class, userId, trainerId);
        return follow != null;
    }

    public List<LearnerFollow> findByUserId(String userId) {
        LearnerFollow hashKeyValues = new LearnerFollow();
        hashKeyValues.setUserId(userId);

        DynamoDBQueryExpression<LearnerFollow> queryExpression = new DynamoDBQueryExpression<LearnerFollow>()
                .withHashKeyValues(hashKeyValues);

        return dynamoDBMapper.query(LearnerFollow.class, queryExpression);
    }
}
