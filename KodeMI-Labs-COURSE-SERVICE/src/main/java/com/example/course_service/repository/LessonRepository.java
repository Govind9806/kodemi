package com.example.course_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.model.LessonEntity;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class LessonRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public LessonRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(LessonEntity lesson) {
        dynamoDBMapper.save(lesson);
    }

    public LessonEntity findById(String lessonId) {
        return dynamoDBMapper.load(LessonEntity.class, lessonId);
    }

    public List<LessonEntity> findAll() {
        return dynamoDBMapper.scan(LessonEntity.class, new DynamoDBScanExpression());
    }

    public List<LessonEntity> findByModuleId(String moduleId) {
        LessonEntity key = new LessonEntity();
        key.setModuleId(moduleId);
        DynamoDBQueryExpression<LessonEntity> query =
                new DynamoDBQueryExpression<LessonEntity>()
                        .withIndexName("module-index")
                        .withConsistentRead(false)
                        .withHashKeyValues(key);
        return dynamoDBMapper.query(LessonEntity.class, query);
    }

    public void delete(LessonEntity lesson) {
        dynamoDBMapper.delete(lesson);
    }
}