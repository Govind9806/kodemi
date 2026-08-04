package com.example.enrollment_progress_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.enrollment_progress_service.enums.EnrollmentStatus;
import com.example.enrollment_progress_service.model.EnrollmentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class EnrollmentRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public void save(EnrollmentEntity entity) {
        dynamoDBMapper.save(entity);
    }

    public EnrollmentEntity findByEnrollmentId(String enrollmentId) {
        return dynamoDBMapper.load(EnrollmentEntity.class, enrollmentId);
    }


    public List<EnrollmentEntity> findByUserId(String userId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS(userId));

        DynamoDBQueryExpression<EnrollmentEntity> queryExpression = new DynamoDBQueryExpression<EnrollmentEntity>()
                .withIndexName("user-index")
                .withConsistentRead(false)
                .withKeyConditionExpression("userId = :val1")
                .withExpressionAttributeValues(eav);

        List<EnrollmentEntity> result = dynamoDBMapper.query(EnrollmentEntity.class, queryExpression);
        if (result == null || result.isEmpty()) {
            DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                    .withFilterExpression("userId = :val1")
                    .withExpressionAttributeValues(eav);
            result = dynamoDBMapper.scan(EnrollmentEntity.class, scanExpression);
        }
        return result;
    }


    public List<EnrollmentEntity> findByCreatorId(String creatorId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS(creatorId));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("creatorId = :val1")
                .withExpressionAttributeValues(eav);

        return dynamoDBMapper.scan(EnrollmentEntity.class, scanExpression);
    }


    public List<EnrollmentEntity> findByUserIdAndCreatorId(String userId, String creatorId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS(userId));
        eav.put(":val2", new AttributeValue().withS(creatorId));

        DynamoDBQueryExpression<EnrollmentEntity> queryExpression = new DynamoDBQueryExpression<EnrollmentEntity>()
                .withIndexName("user-index")
                .withConsistentRead(false)
                .withKeyConditionExpression("userId = :val1 AND creatorId = :val2")
                .withExpressionAttributeValues(eav);

        return dynamoDBMapper.query(EnrollmentEntity.class, queryExpression);
    }

    public List<EnrollmentEntity> findByTargetId(String targetId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS(targetId));

        DynamoDBQueryExpression<EnrollmentEntity> queryExpression = new DynamoDBQueryExpression<EnrollmentEntity>()
                .withIndexName("target-index")
                .withConsistentRead(false)
                .withKeyConditionExpression("targetId = :val1")
                .withExpressionAttributeValues(eav);

        return dynamoDBMapper.query(EnrollmentEntity.class, queryExpression);
    }

    public List<EnrollmentEntity> findByUserIdAndTargetIdAndTargetType(String userId, String targetId, String targetType) {
        List<EnrollmentEntity> userEnrollments = findByUserId(userId);
        List<EnrollmentEntity> result = new ArrayList<>();
        if (userEnrollments != null) {
            for (EnrollmentEntity e : userEnrollments) {
                if (e != null && e.getTargetId() != null && e.getTargetType() != null &&
                        e.getTargetId().equals(targetId) && e.getTargetType().name().equals(targetType)) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    public EnrollmentEntity findActiveEnrollment(String userId, String targetId, String targetType) {
        List<EnrollmentEntity> enrollments = findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
        if (enrollments != null) {
            for (EnrollmentEntity e : enrollments) {
                if (e != null && EnrollmentStatus.ACTIVE.equals(e.getStatus())) {
                    return e;
                }
            }
        }
        return null;
    }
}