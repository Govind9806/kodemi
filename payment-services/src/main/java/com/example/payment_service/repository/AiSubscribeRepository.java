package com.example.payment_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.payment_service.model.AiSubscribe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AiSubscribeRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public AiSubscribe save(AiSubscribe subscription) {
        dynamoDBMapper.save(subscription);
        return subscription;
    }

    public Optional<AiSubscribe> findById(String subscriptionId) {
        return Optional.ofNullable(dynamoDBMapper.load(AiSubscribe.class, subscriptionId));
    }

    public List<AiSubscribe> findByUserId(String userId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":userId", new AttributeValue().withS(userId));

        DynamoDBQueryExpression<AiSubscribe> queryExpression = new DynamoDBQueryExpression<AiSubscribe>()
                .withIndexName("user-index")
                .withConsistentRead(false)
                .withKeyConditionExpression("userId = :userId")
                .withExpressionAttributeValues(eav);

        return dynamoDBMapper.query(AiSubscribe.class, queryExpression);
    }

    public List<AiSubscribe> findAll() {
        return dynamoDBMapper.scan(AiSubscribe.class, new DynamoDBScanExpression());
    }

    public void delete(AiSubscribe subscription) {
        dynamoDBMapper.delete(subscription);
    }

    public void deleteById(String subscriptionId) {
        Optional<AiSubscribe> opt = findById(subscriptionId);
        if (opt.isPresent()) {
            dynamoDBMapper.delete(opt.get());
        }
    }
}
