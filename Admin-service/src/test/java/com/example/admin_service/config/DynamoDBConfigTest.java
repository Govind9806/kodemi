package com.example.admin_service.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DynamoDBConfigTest {

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Test
    void amazonDynamoDBBeanShouldBeCreated() {
        assertNotNull(amazonDynamoDB, "AmazonDynamoDB bean should not be null");
    }

    @Test
    void dynamoDBMapperBeanShouldBeCreated() {
        assertNotNull(dynamoDBMapper, "DynamoDBMapper bean should not be null");
    }

    @Test
    void dynamoDBMapperIsOfCorrectType() {
        assertEquals(DynamoDBMapper.class, dynamoDBMapper.getClass(), "DynamoDBMapper bean should be of correct class");
    }

    // Test configuration for local DynamoDB
    @Configuration
    static class TestDynamoDBConfig {

        @Bean
        public AmazonDynamoDB amazonDynamoDB() {
            return com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-east-1"))
                    .withCredentials(new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials("test-access-key", "test-secret-key")))
                    .build();
        }

        @Bean
        public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
            return new DynamoDBMapper(amazonDynamoDB);
        }
    }
}