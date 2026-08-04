package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.model.ReviewEntity;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class ReviewRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public ReviewRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(ReviewEntity review) {
        dynamoDBMapper.save(review);
    }

    public ReviewEntity findById(String reviewId) {
        return dynamoDBMapper.load(ReviewEntity.class, reviewId);
    }

    public List<ReviewEntity> findAll() {
        return dynamoDBMapper.scan(ReviewEntity.class, new DynamoDBScanExpression());
    }

    public List<ReviewEntity> findByCourseId(String courseId) {
        ReviewEntity key = new ReviewEntity();
        key.setCourseId(courseId);
        DynamoDBQueryExpression<ReviewEntity> query =
                new DynamoDBQueryExpression<ReviewEntity>()
                        .withIndexName("course-index")
                        .withConsistentRead(false)
                        .withHashKeyValues(key);
        return dynamoDBMapper.query(ReviewEntity.class, query);
    }
}