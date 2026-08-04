package com.example.payment_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.payment_service.model.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class WalletTransactionRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public WalletTransaction save(WalletTransaction transaction) {
        dynamoDBMapper.save(transaction);
        return transaction;
    }

    public Optional<WalletTransaction> findById(String transactionId) {
        WalletTransaction transaction = dynamoDBMapper.load(WalletTransaction.class, transactionId);
        return Optional.ofNullable(transaction);
    }

    public List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(String userId) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":userId", new AttributeValue().withS(userId));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("userId = :userId")
                .withExpressionAttributeValues(values);

        List<WalletTransaction> paginatedList = dynamoDBMapper.scan(WalletTransaction.class, scanExpression);
        List<WalletTransaction> list = new ArrayList<>(paginatedList);
        list.sort(new Comparator<WalletTransaction>() {
            @Override
            public int compare(WalletTransaction o1, WalletTransaction o2) {
                if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
                if (o1.getCreatedAt() == null) return 1;
                if (o2.getCreatedAt() == null) return -1;
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            }
        });
        return list;
    }

    public List<WalletTransaction> findByTransactionType(String transactionType) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":transactionType", new AttributeValue().withS(transactionType));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("transactionType = :transactionType")
                .withExpressionAttributeValues(values);

        return dynamoDBMapper.scan(WalletTransaction.class, scanExpression);
    }

    public List<WalletTransaction> findByStatus(String status) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":status", new AttributeValue().withS(status));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("status = :status")
                .withExpressionAttributeValues(values);

        return dynamoDBMapper.scan(WalletTransaction.class, scanExpression);
    }

    public List<WalletTransaction> findByRazorpayOrderId(String razorpayOrderId) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":razorpayOrderId", new AttributeValue().withS(razorpayOrderId));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("razorpayOrderId = :razorpayOrderId")
                .withExpressionAttributeValues(values);

        return dynamoDBMapper.scan(WalletTransaction.class, scanExpression);
    }

    public List<WalletTransaction> findAll() {
        return dynamoDBMapper.scan(WalletTransaction.class, new DynamoDBScanExpression());
    }

    public List<WalletTransaction> findByUserIdAndTransactionType(String userId, String transactionType) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":userId", new AttributeValue().withS(userId));
        values.put(":transactionType", new AttributeValue().withS(transactionType));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("userId = :userId AND transactionType = :transactionType")
                .withExpressionAttributeValues(values);

        return dynamoDBMapper.scan(WalletTransaction.class, scanExpression);
    }

    /**
     * CHANGE COMMENT: NEW METHOD ADDED FOR TRAINER REVENUE ANALYTICS
     * 
     * Purpose: Query wallet transactions by userId and multiple transaction types
     * This method is used to filter earnings transactions for trainers
     * 
     * @param userId - The trainer's user ID
     * @param transactionTypes - List of transaction types to filter (e.g., EARNING, BUY_COURSE, BUY_RESOURCE)
     * @return List of wallet transactions sorted by createdAt (newest first)
     * 
     * Used by: WalletService.getTrainerRevenueAnalytics()
     */
    public List<WalletTransaction> findByUserIdAndTransactionTypes(String userId, List<String> transactionTypes) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":userId", new AttributeValue().withS(userId));
        
        // Build DynamoDB IN clause for multiple transaction types
        // Example: transactionType IN (:type0, :type1, :type2)
        StringBuilder inClause = new StringBuilder("transactionType IN (");
        for (int i = 0; i < transactionTypes.size(); i++) {
            values.put(":type" + i, new AttributeValue().withS(transactionTypes.get(i)));
            if (i > 0) inClause.append(", ");
            inClause.append(":type").append(i);
        }
        inClause.append(")");

        // Create scan expression with userId and transaction type filters
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("userId = :userId AND " + inClause.toString())
                .withExpressionAttributeValues(values);

        List<WalletTransaction> paginatedList = dynamoDBMapper.scan(WalletTransaction.class, scanExpression);
        List<WalletTransaction> list = new ArrayList<>(paginatedList);
        
        // Sort by createdAt in descending order (newest transactions first)
        list.sort((o1, o2) -> {
            if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) return 0;
            if (o1.getCreatedAt() == null) return 1;
            if (o2.getCreatedAt() == null) return -1;
            return o2.getCreatedAt().compareTo(o1.getCreatedAt());
        });
        
        return list;
    }
}