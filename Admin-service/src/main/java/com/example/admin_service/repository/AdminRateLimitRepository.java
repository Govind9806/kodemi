package com.example.admin_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.admin_service.model.AdminRateLimit;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRateLimitRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public AdminRateLimitRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public AdminRateLimit getAdminRateLimit(String adminIdDate) {
        return dynamoDBMapper.load(AdminRateLimit.class, adminIdDate);
    }

    public void save(AdminRateLimit rateLimit) {
        dynamoDBMapper.save(rateLimit);
    }
}
