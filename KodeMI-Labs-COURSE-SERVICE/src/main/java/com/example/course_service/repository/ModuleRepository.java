package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.model.ModuleEntity;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class ModuleRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public ModuleRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(ModuleEntity module) {
        dynamoDBMapper.save(module);
    }

    public ModuleEntity findById(String moduleId) {
        return dynamoDBMapper.load(ModuleEntity.class, moduleId);
    }

    public List<ModuleEntity> findAll() {
        return dynamoDBMapper.scan(ModuleEntity.class, new DynamoDBScanExpression());
    }

    public List<ModuleEntity> findByCourseId(String courseId) {
        ModuleEntity key = new ModuleEntity();
        key.setCourseId(courseId);
        DynamoDBQueryExpression<ModuleEntity> query =
                new DynamoDBQueryExpression<ModuleEntity>()
                        .withIndexName("course-index")
                        .withConsistentRead(false)
                        .withHashKeyValues(key);
        return dynamoDBMapper.query(ModuleEntity.class, query);
    }
}