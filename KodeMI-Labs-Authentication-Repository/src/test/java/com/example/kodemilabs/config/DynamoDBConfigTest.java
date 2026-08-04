package com.example.kodemilabs.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class DynamoDBConfigTest {

    @Test
    void testDynamoDBMapperCreation() {
        DynamoDBConfig config = new DynamoDBConfig();
        AmazonDynamoDB mockClient = mock(AmazonDynamoDB.class);
        DynamoDBMapper mapper = config.dynamoDBMapper(mockClient);
        assertNotNull(mapper);
    }
}
