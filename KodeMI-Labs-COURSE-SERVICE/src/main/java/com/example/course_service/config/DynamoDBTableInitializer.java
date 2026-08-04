package com.example.course_service.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DynamoDBTableInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBTableInitializer.class);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int MAX_POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MILLIS = 1000L;
    private static final long DEFAULT_READ_WRITE_CAPACITY = 5L;

    private final AmazonDynamoDB amazonDynamoDB;
    private final DynamoDBMapper dynamoDBMapper;

    public DynamoDBTableInitializer(AmazonDynamoDB amazonDynamoDB, DynamoDBMapper dynamoDBMapper) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.dynamoDBMapper = dynamoDBMapper;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing DynamoDB tables...");
        createTableIfNotExists(com.example.course_service.model.CategoryEntity.class);
        createTableIfNotExists(com.example.course_service.model.CourseEntity.class);
        createTableIfNotExists(com.example.course_service.model.LessonEntity.class);
        createTableIfNotExists(com.example.course_service.model.LiveCourseMapping.class);
        createTableIfNotExists(com.example.course_service.model.ModuleEntity.class);
        createTableIfNotExists(com.example.course_service.model.ReviewEntity.class);
        createTableIfNotExists(com.example.course_service.model.UploadEntity.class);
        log.info("DynamoDB tables initialization check complete.");
    }

    private void createTableIfNotExists(Class<?> entityClass) {
        try {
            CreateTableRequest createTableRequest = dynamoDBMapper.generateCreateTableRequest(entityClass);
            String tableName = createTableRequest.getTableName();

            if (tableAlreadyExists(tableName)) {
                return;
            }

            applyProvisionedThroughput(createTableRequest);

            amazonDynamoDB.createTable(createTableRequest);
            log.info("DynamoDB table '{}' creation request submitted. Waiting for it to become ACTIVE...", tableName);

            waitForTableToBecomeActive(tableName);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while checking/creating DynamoDB table for class {}",
                    entityClass.getSimpleName(), e);
        } catch (Exception e) {
            log.error("Failed to check/create DynamoDB table for class {}: {}",
                    entityClass.getSimpleName(), e.getMessage(), e);
        }
    }


    private boolean tableAlreadyExists(String tableName) {
        try {
            DescribeTableResult describeTableResult = amazonDynamoDB.describeTable(tableName);
            log.info("DynamoDB table '{}' already exists with status: {}",
                    tableName,
                    describeTableResult.getTable().getTableStatus());
            return true;
        } catch (ResourceNotFoundException e) {
            log.info("DynamoDB table '{}' not found. Creating table...", tableName);
            return false;
        }
    }

    private void applyProvisionedThroughput(CreateTableRequest createTableRequest) {
        createTableRequest.setProvisionedThroughput(
                new ProvisionedThroughput(DEFAULT_READ_WRITE_CAPACITY, DEFAULT_READ_WRITE_CAPACITY));

        if (createTableRequest.getGlobalSecondaryIndexes() != null) {
            for (GlobalSecondaryIndex index : createTableRequest.getGlobalSecondaryIndexes()) {
                index.setProvisionedThroughput(
                        new ProvisionedThroughput(DEFAULT_READ_WRITE_CAPACITY, DEFAULT_READ_WRITE_CAPACITY));
            }
        }
    }

    private void waitForTableToBecomeActive(String tableName) throws InterruptedException {
        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MILLIS);

            if (isTableActive(tableName)) {
                log.info("DynamoDB table '{}' is now ACTIVE.", tableName);
                return;
            }
        }

        log.warn("Timed out waiting for DynamoDB table '{}' to become ACTIVE.", tableName);
    }


    private boolean isTableActive(String tableName) {
        try {
            DescribeTableResult describeResult = amazonDynamoDB.describeTable(tableName);
            return STATUS_ACTIVE.equalsIgnoreCase(describeResult.getTable().getTableStatus());
        } catch (Exception ex) {
            // Ignore transient errors while waiting for the table to become ACTIVE
            log.debug("Waiting for DynamoDB table '{}' to become ACTIVE: {}", tableName, ex.getMessage());
            return false;
        }
    }
}