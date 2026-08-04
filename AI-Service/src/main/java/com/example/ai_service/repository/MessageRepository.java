package com.example.ai_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.ai_service.model.Messages;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MessageRepository {
    private final DynamoDBMapper dynamoDBMapper;

    public MessageRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(Messages messages) {
        dynamoDBMapper.save(messages);
    }

    public Messages findById(String messageId) {
        return dynamoDBMapper.load(Messages.class, messageId);
    }

    public List<Messages> findAllMessagesByConversation(String conversationId) {
        /*
         * The 'conversation-index' GSI in the actual DynamoDB table has createdAt
         * as its HASH key (opposite of the model annotation), so any Query on that
         * index fails with:
         *   "HASH key attributes createdAt must have equality conditions"
         *
         * Fix: use a full-table Scan with a FilterExpression on conversationId.
         * Results are then sorted by createdAt ascending in Java to replicate the
         * original withScanIndexForward(true) behaviour.
         *
         * Long-term: recreate the GSI with conversationId as HASH key and
         * createdAt as RANGE key in the AWS console, then restore the Query.
         */
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":cid", new AttributeValue().withS(conversationId));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("conversationId = :cid")
                .withExpressionAttributeValues(eav);

        List<Messages> results = dynamoDBMapper.scan(Messages.class, scanExpression);

        // Sort ascending by createdAt (ISO-8601 strings sort lexicographically)
        return results.stream()
                .sorted(Comparator.comparing(
                        m -> m.getCreatedAt() != null ? m.getCreatedAt() : "",
                        Comparator.naturalOrder()
                ))
                .toList();
    }

    public void deleteMessage(Messages messages) {
        dynamoDBMapper.delete(messages);
    }
}

