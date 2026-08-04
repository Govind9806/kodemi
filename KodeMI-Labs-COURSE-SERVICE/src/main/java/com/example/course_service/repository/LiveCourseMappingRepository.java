package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.course_service.model.LiveCourseMapping;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class LiveCourseMappingRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public LiveCourseMappingRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(LiveCourseMapping mapping) {
        dynamoDBMapper.save(mapping);
    }

    public LiveCourseMapping findById(String id) {
        return dynamoDBMapper.load(LiveCourseMapping.class, id);
    }

    public LiveCourseMapping findByCourseId(String courseId) {
        if (courseId == null) return null;

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(courseId));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("courseId = :v1")
                .withExpressionAttributeValues(eav);

        List<LiveCourseMapping> results = dynamoDBMapper.scan(LiveCourseMapping.class, scanExpression);
        return results.isEmpty() ? null : results.get(0);
    }

    public void delete(String id) {
        LiveCourseMapping mapping = findById(id);
        if (mapping != null) {
            dynamoDBMapper.delete(mapping);
        }
    }
}
