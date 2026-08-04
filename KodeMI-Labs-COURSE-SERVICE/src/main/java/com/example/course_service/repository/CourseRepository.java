package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.course_service.model.CourseEntity;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public class CourseRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public CourseRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(CourseEntity course) {
        dynamoDBMapper.save(course);
    }

    public CourseEntity findById(String courseId) {
        return dynamoDBMapper.load(CourseEntity.class, courseId);
    }

    public List<CourseEntity> findAll() {
        DynamoDBScanExpression scan = new DynamoDBScanExpression().withLimit(100);
        return dynamoDBMapper.scanPage(CourseEntity.class, scan).getResults();
    }

    public List<CourseEntity> findByIsVerified(Boolean isVerified) {
        DynamoDBScanExpression scan = new DynamoDBScanExpression()
                .withFilterExpression("isVerified = :v")
                .withExpressionAttributeValues(Map.of(
                        ":v", new AttributeValue().withBOOL(isVerified)))
                .withLimit(100);
        return dynamoDBMapper.scanPage(CourseEntity.class, scan).getResults();
    }

    public List<CourseEntity> findByCategoryId(String categoryId) {
        DynamoDBScanExpression scan = new DynamoDBScanExpression()
                .withFilterExpression("categoryId = :c AND isVerified = :v")
                .withExpressionAttributeValues(Map.of(
                        ":c", new AttributeValue().withS(categoryId),
                        ":v", new AttributeValue().withBOOL(true)))
                .withLimit(100);
        return dynamoDBMapper.scanPage(CourseEntity.class, scan).getResults();
    }
}