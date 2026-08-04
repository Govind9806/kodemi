package com.example.payment_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.payment_service.model.PayoutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class PayoutRequestRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public PayoutRequest save(PayoutRequest payoutRequest) {
        dynamoDBMapper.save(payoutRequest);
        return payoutRequest;
    }

    public Optional<PayoutRequest> findById(String payoutId) {
        PayoutRequest request = dynamoDBMapper.load(PayoutRequest.class, payoutId);
        return Optional.ofNullable(request);
    }

    // ✅ Optimized using GSI: trainerId-index
    public List<PayoutRequest> findByTrainerIdOrderByRequestedAtDesc(String trainerId) {

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":trainerId", new AttributeValue().withS(trainerId));

        DynamoDBQueryExpression<PayoutRequest> query =
                new DynamoDBQueryExpression<PayoutRequest>()
                        .withIndexName("trainerId-index")
                        .withConsistentRead(false)
                        .withKeyConditionExpression("trainerId = :trainerId")
                        .withExpressionAttributeValues(values)
                        .withScanIndexForward(false); // DESC order

        return dynamoDBMapper.query(PayoutRequest.class, query);
    }

    public List<PayoutRequest> findByStatusOrderByRequestedAtDesc(String status) {

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":status", new AttributeValue().withS(status));

        Map<String, String> names = new HashMap<>();
        names.put("#status", "status");

        DynamoDBQueryExpression<PayoutRequest> query =
                new DynamoDBQueryExpression<PayoutRequest>()
                        .withIndexName("status-index")
                        .withConsistentRead(false)
                        .withKeyConditionExpression("#status = :status")
                        .withExpressionAttributeNames(names)
                        .withExpressionAttributeValues(values)
                        .withScanIndexForward(false); // DESC order

        return dynamoDBMapper.query(PayoutRequest.class, query);
    }

    // ⚠️ Still Scan (no index for global sorting)
    public List<PayoutRequest> findAllByOrderByRequestedAtDesc() {

        List<PayoutRequest> paginatedList = dynamoDBMapper.scan(
                PayoutRequest.class,
                new DynamoDBScanExpression()
        );
        List<PayoutRequest> list = new ArrayList<>(paginatedList);

        list.sort(new Comparator<PayoutRequest>() {
            @Override
            public int compare(PayoutRequest o1, PayoutRequest o2) {
                if (o1.getRequestedAt() == null && o2.getRequestedAt() == null) return 0;
                if (o1.getRequestedAt() == null) return 1;
                if (o2.getRequestedAt() == null) return -1;
                return o2.getRequestedAt().compareTo(o1.getRequestedAt());
            }
        });
        return list;
    }
    public List<PayoutRequest> findAll() {
        return dynamoDBMapper.scan(PayoutRequest.class, new DynamoDBScanExpression());
    }

    public void delete(String payoutId) {
        PayoutRequest request = dynamoDBMapper.load(PayoutRequest.class, payoutId);
        if (request != null) {
            dynamoDBMapper.delete(request);
        }
    }
}