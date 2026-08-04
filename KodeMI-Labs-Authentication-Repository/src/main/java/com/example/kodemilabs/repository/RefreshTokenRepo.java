package com.example.kodemilabs.repository;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.kodemilabs.model.RefreshToken;
import org.springframework.stereotype.Repository;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import java.util.*;


import java.util.HashMap;

@Repository
public class RefreshTokenRepo {

    private final DynamoDBMapper dynamoDBMapper;

    public RefreshTokenRepo(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(RefreshToken token){
        dynamoDBMapper.save(token);
    }

    public RefreshToken find(String token){
        return dynamoDBMapper.load(RefreshToken.class, token);
    }

    public void delete (String token){

        RefreshToken rt =
                dynamoDBMapper.load(RefreshToken.class, token);

        if(rt != null){
            dynamoDBMapper.delete(rt);
        }
    }
    
    /**
     * Find refresh token by email using GSI query (not scan).
     * NOTE: Requires GSI named "email-index" on the email attribute.
     */
    public RefreshToken findByEmail(String email){

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":email", new AttributeValue().withS(email));

        DynamoDBQueryExpression<RefreshToken> query =
                new DynamoDBQueryExpression<RefreshToken>()
                        .withIndexName("email-index")
                        .withConsistentRead(false)
                        .withKeyConditionExpression("email = :email")
                        .withExpressionAttributeValues(eav)
                        .withLimit(1);

        List<RefreshToken> result =
                dynamoDBMapper.query(RefreshToken.class, query);

        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Find all refresh tokens by email using GSI query.
     */
    public List<RefreshToken> findAllByEmail(String email){
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":email", new AttributeValue().withS(email));

        DynamoDBQueryExpression<RefreshToken> query =
                new DynamoDBQueryExpression<RefreshToken>()
                        .withIndexName("email-index")
                        .withConsistentRead(false)
                        .withKeyConditionExpression("email = :email")
                        .withExpressionAttributeValues(eav);

        return dynamoDBMapper.query(RefreshToken.class, query);
    }

    /**
     * Revokes (deletes) all refresh tokens for a given email.
     */
    public void deleteAllByEmail(String email) {
        List<RefreshToken> tokens = findAllByEmail(email);
        if (tokens != null && !tokens.isEmpty()) {
            dynamoDBMapper.batchDelete(tokens);
        }
    }
}
