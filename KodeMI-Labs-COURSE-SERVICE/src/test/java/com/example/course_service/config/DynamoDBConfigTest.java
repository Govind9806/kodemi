package com.example.course_service.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DynamoDBConfigTest {

    @Test
    void dynamoDBBeans_Creation_Success() {
        DynamoDBConfig config = new DynamoDBConfig();

        // Inject region manually for testing
        ReflectionTestUtils.setField(config, "dynamodbRegion", "eu-north-1");

        // Create AmazonDynamoDB bean
        AmazonDynamoDB amazonDynamoDB = config.amazonDynamoDB();
        assertNotNull(amazonDynamoDB, "AmazonDynamoDB bean should not be null");

        // Create DynamoDBMapper bean
        DynamoDBMapper mapper = config.dynamoDBMapper(amazonDynamoDB);
        assertNotNull(mapper, "DynamoDBMapper bean should not be null");

        // Optional: check that the mapper is instance of DynamoDBMapper
        assertTrue(mapper instanceof DynamoDBMapper);
    }
}