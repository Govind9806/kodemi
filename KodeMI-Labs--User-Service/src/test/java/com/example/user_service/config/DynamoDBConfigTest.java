package com.example.user_service.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class DynamoDBConfigTest {

    private DynamoDBConfig dynamoDBConfig;

    @BeforeEach
    void setUp() {
        dynamoDBConfig = new DynamoDBConfig();
        ReflectionTestUtils.setField(dynamoDBConfig, "dynamodbRegion", "us-east-1");
    }

    @Test
    void dynamoDBMapperBeanShouldBeCreated() {
        AmazonDynamoDB mockClient = mock(AmazonDynamoDB.class);
        DynamoDBMapper dynamoDBMapper = dynamoDBConfig.dynamoDBMapper(mockClient);
        assertNotNull(dynamoDBMapper, "DynamoDBMapper bean should not be null");
    }

    @Test
    void dynamoDBMapperIsOfCorrectType() {
        AmazonDynamoDB mockClient = mock(AmazonDynamoDB.class);
        DynamoDBMapper dynamoDBMapper = dynamoDBConfig.dynamoDBMapper(mockClient);
        assertEquals(DynamoDBMapper.class, dynamoDBMapper.getClass(), "DynamoDBMapper bean should be of correct class");
    }

    @Test
    void amazonDynamoDBBeanShouldBeCreated() {
        AmazonDynamoDB client = dynamoDBConfig.amazonDynamoDB();
        assertNotNull(client, "AmazonDynamoDB bean should not be null");
    }
}