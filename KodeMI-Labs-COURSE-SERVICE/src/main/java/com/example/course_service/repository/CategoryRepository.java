package com.example.course_service.repository;


import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.example.course_service.model.CategoryEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryRepository {
    private final DynamoDBMapper dynamoDBMapper;
    public CategoryRepository(DynamoDBMapper dynamoDBMapper){
        this.dynamoDBMapper=dynamoDBMapper;
    }

    public void save(CategoryEntity category){
        dynamoDBMapper.save(category);
    }
    public List<CategoryEntity> findAll() {
        return dynamoDBMapper.scan(CategoryEntity.class, new DynamoDBScanExpression());
    }

    public void delete(CategoryEntity category) {
        dynamoDBMapper.delete(category);
    }

    public CategoryEntity findById(String categoryId){
        return dynamoDBMapper.load(CategoryEntity.class,categoryId);
    }

    public CategoryEntity findByName(String name) {
        CategoryEntity hashKeyValues = new CategoryEntity();
        hashKeyValues.setName(name);

        DynamoDBQueryExpression<CategoryEntity> queryExpression = new DynamoDBQueryExpression<CategoryEntity>()
                .withIndexName("name-index")
                .withHashKeyValues(hashKeyValues)
                .withConsistentRead(false);

        List<CategoryEntity> results = dynamoDBMapper.query(CategoryEntity.class, queryExpression);
        return results.isEmpty() ? null : results.get(0);
    }
}