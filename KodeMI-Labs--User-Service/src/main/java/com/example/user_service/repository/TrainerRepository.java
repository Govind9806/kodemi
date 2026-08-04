package com.example.user_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.example.user_service.model.Trainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TrainerRepository {

    private final DynamoDBMapper dynamoDBMapper;
    private static final int MAX_SCAN_LIMIT = 100;

    public void save(Trainer trainerProfile) {
        dynamoDBMapper.save(trainerProfile);
    }
    
    public Trainer findById(String userId) {
        return dynamoDBMapper.load(Trainer.class, userId);
    }

    public List<Trainer> findByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<Trainer> trainersToGet = new java.util.ArrayList<>();
        for (String id : userIds) {
            Trainer t = new Trainer();
            t.setUserId(id);
            trainersToGet.add(t);
        }
        java.util.Map<String, List<Object>> results = dynamoDBMapper.batchLoad(trainersToGet);
        List<Trainer> trainers = new java.util.ArrayList<>();
        for (List<Object> list : results.values()) {
            for (Object obj : list) {
                if (obj instanceof Trainer trainer) {
                    trainers.add(trainer);
                }
            }
        }
        return trainers;
    }

    public List<Trainer> findAll() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withLimit(MAX_SCAN_LIMIT);
        
        List<Trainer> trainers = dynamoDBMapper.scan(Trainer.class, scanExpression);
        log.debug("Scanned {} trainers from DynamoDB", trainers.size());
        return trainers;
    }

    public void delete(String userId) {
        Trainer trainerProfile = dynamoDBMapper.load(Trainer.class, userId);
        if(trainerProfile != null) {
            dynamoDBMapper.delete(trainerProfile);
        }
    }
}

