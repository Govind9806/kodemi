package com.example.ai_service.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.ai_service.model.Conversations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ConversationRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public void save(Conversations conversations){
        dynamoDBMapper.save(conversations);
    }

    public Conversations getConversationById(String conversationId){
        return dynamoDBMapper.load(Conversations.class, conversationId);
    }

    public List<Conversations> getUserConversations(String userId) {

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":uid", new AttributeValue().withS(userId));

        DynamoDBQueryExpression<Conversations> query = new DynamoDBQueryExpression<Conversations>()
                .withIndexName("user-index")
                .withConsistentRead(false)
                .withKeyConditionExpression("userId = :uid")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(true);

        return dynamoDBMapper.query(Conversations.class, query);
    }

    public void delete(Conversations conversations){
        dynamoDBMapper.delete(conversations);
    }
}
