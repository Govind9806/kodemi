package com.example.payment_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.payment_service.model.Payment;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;

@Repository
public class PaymentRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public PaymentRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(Payment payment) {
        dynamoDBMapper.save(payment);
    }

    public Optional<Payment> findById(String orderId) {
        Payment payment = dynamoDBMapper.load(Payment.class, orderId);
        return Optional.ofNullable(payment);
    }

    public Payment findByOrderId(String orderId) {
        return dynamoDBMapper.load(Payment.class, orderId);
    }

    public void update(Payment payment) {
        dynamoDBMapper.save(payment); // save() acts as update as well
    }

    public void delete(String orderId) {
        Payment payment = dynamoDBMapper.load(Payment.class, orderId);
        if (payment != null) {
            dynamoDBMapper.delete(payment);
        }
    }

    public List<Payment> findByUserId(String userId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withS(userId));

        DynamoDBQueryExpression<Payment> queryExpression = new DynamoDBQueryExpression<Payment>()
                .withIndexName("userId-index")
                .withConsistentRead(false)
                .withKeyConditionExpression("userId = :val1")
                .withExpressionAttributeValues(eav);

        return dynamoDBMapper.query(Payment.class, queryExpression);
    }
}