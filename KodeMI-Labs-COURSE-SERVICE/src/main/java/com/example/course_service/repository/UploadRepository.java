package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.model.UploadEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UploadRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public UploadRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(UploadEntity upload) {
        dynamoDBMapper.save(upload);
    }

    public UploadEntity findById(String uploadId) {
        return dynamoDBMapper.load(UploadEntity.class, uploadId);
    }

    public List<UploadEntity> findAll() {
        return dynamoDBMapper.scan(UploadEntity.class, new DynamoDBScanExpression());
    }

    public void delete(UploadEntity upload) {
        dynamoDBMapper.delete(upload);
    }
}
