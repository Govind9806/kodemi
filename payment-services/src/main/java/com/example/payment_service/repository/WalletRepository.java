package com.example.payment_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.payment_service.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import com.amazonaws.services.dynamodbv2.datamodeling.TransactionWriteRequest;

@Repository
public class WalletRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public WalletRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public Wallet save(Wallet wallet) {
        dynamoDBMapper.save(wallet);
        return wallet;
    }

    public void executeTransaction(Object... items) {
        TransactionWriteRequest request = new TransactionWriteRequest();
        for (Object item : items) {
            request.addPut(item);
        }
        dynamoDBMapper.transactionWrite(request);
    }

    public Optional<Wallet> findByUserId(String userId) {
        Wallet wallet = dynamoDBMapper.load(Wallet.class, userId);
        return Optional.ofNullable(wallet);
    }

    // ✅ Optimized using GSI: userType-index
    public List<Wallet> findByUserType(String userType) {

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":userType", new AttributeValue().withS(userType));

        DynamoDBQueryExpression<Wallet> query =
                new DynamoDBQueryExpression<Wallet>()
                        .withIndexName("userType-index")
                        .withConsistentRead(false)
                        .withKeyConditionExpression("userType = :userType")
                        .withExpressionAttributeValues(values);

        return dynamoDBMapper.query(Wallet.class, query);
    }

    // ⚠️ Still Scan (no index on status)
    public List<Wallet> findByStatus(String status) {

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":status", new AttributeValue().withS(status));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("status = :status")
                .withExpressionAttributeValues(values);

        return dynamoDBMapper.scan(Wallet.class, scanExpression);
    }

    // ⚠️ Full table scan
    public List<Wallet> findAll() {
        return dynamoDBMapper.scan(Wallet.class, new DynamoDBScanExpression());
    }

    public void delete(String userId) {
        Wallet wallet = dynamoDBMapper.load(Wallet.class, userId);
        if (wallet != null) {
            dynamoDBMapper.delete(wallet);
        }
    }
}