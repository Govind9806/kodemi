package com.example.payment_service.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Enhanced DynamoDB Configuration with AWS SDK v2 Support
 * 
 * WHAT: Configures DynamoDB connectivity using both AWS SDK v1 and v2
 * WHY: v2 for modern features, v1 for backward compatibility
 * HOW: Uses DefaultCredentialsProvider which checks:
 *      1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 *      2. AWS credentials file (~/.aws/credentials)
 *      3. IAM role (if running on EC2/Lambda/ECS)
 * WHERE: Used globally for all DynamoDB operations
 */
@Configuration
@Slf4j
public class DynamoDbConfig {

    @Value("${dynamodb.region:us-east-1}")
    private String dynamodbRegion;

    @Value("${dynamodb.endpoint:}")
    private String dynamodbEndpoint;

    @Value("${app.env:dev}")
    private String appEnv;

    /**
     * Bean for AWS SDK v1 DynamoDB Client (backward compatibility)
     * Used by DynamoDBMapper for existing code
     */
    @Bean
    public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
        log.info("Initializing DynamoDBMapper for region: {}", dynamodbRegion);
        return new DynamoDBMapper(amazonDynamoDB);
    }

    /**
     * Bean for AWS SDK v1 AmazonDynamoDB Client
     * 
     * CREDENTIALS CHAIN:
     * - DefaultAWSCredentialsProviderChain checks multiple sources:
     *   1. Environment variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
     *   2. Java system properties: aws.accessKeyId, aws.secretKey
     *   3. Credential profiles (~/.aws/credentials): [default] or [payment-service]
     *   4. IAM role metadata (EC2, ECS, Lambda)
     *   5. Web identity (IRSA in Kubernetes)
     */
    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        log.info("Creating AmazonDynamoDB client for region: {}", dynamodbRegion);
        
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard()
                .withRegion(dynamodbRegion)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance());

        // Optional: Use local DynamoDB endpoint (for testing)
        if (dynamodbEndpoint != null && !dynamodbEndpoint.isEmpty()) {
            log.info("Using custom DynamoDB endpoint: {}", dynamodbEndpoint);
            com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
                    endpointConfig = new com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration(
                    dynamodbEndpoint, dynamodbRegion);
            builder.withEndpointConfiguration(endpointConfig);
        }

        return builder.build();
    }

    /**
     * Bean for AWS SDK v2 DynamoDbClient (modern async/modern patterns)
     * Used for new operations and enhanced features
     * 
     * CREDENTIALS RESOLUTION:
     * - DefaultCredentialsProvider follows AWS SDK v2 credential chain
     * - Automatically picks up ~/.aws/credentials file
     * - Falls back to environment variables
     * - Falls back to IAM role if on AWS infrastructure
     */
    @Bean
    public DynamoDbClient dynamoDbClientV2() {
        log.info("Creating DynamoDbClient (AWS SDK v2) for region: {}", dynamodbRegion);
        
        return DynamoDbClient.builder()
                .region(Region.of(dynamodbRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Initialization method to create/verify DynamoDB tables
     * Runs after bean creation
     * 
     * TABLES CREATED:
     * 1. Wallet - trainer and user wallets
     * 2. WalletTransaction - transaction history
     * 3. Payment - payment records
     * 4. Course - course information (if needed)
     * 5. Resource - resource information (if needed)
     */
    @PostConstruct
    public void initializeTables() {
        log.info("=== DynamoDB Table Initialization Started ===");
        log.info("Environment: {}", appEnv);
        log.info("Region: {}", dynamodbRegion);
        log.info("Endpoint: {}", dynamodbEndpoint.isEmpty() ? "AWS Default" : dynamodbEndpoint);

        try {
            DynamoDbClient dynamoDb = dynamoDbClientV2();
            
            // Create required tables
            createTableIfNotExists(dynamoDb, "Wallet");
            createTableIfNotExists(dynamoDb, "WalletTransaction");
            createTableIfNotExists(dynamoDb, "Payment");
            
            log.info("=== DynamoDB Table Initialization Completed ===");
            
        } catch (Exception e) {
            log.error("Error during DynamoDB table initialization", e);
            // Don't fail startup - tables might already exist
        }
    }

    /**
     * Creates a DynamoDB table if it doesn't already exist
     * 
     * PARAMETERS:
     * - dynamoDb: DynamoDbClient to use
     * - tableName: Name of table to create
     * 
     * HOW IT WORKS:
     * 1. Try to describe table - if exists, return
     * 2. If not exists, create with proper schema
     * 3. Wait for table to become active
     * 4. Log success/failure
     */
    private void createTableIfNotExists(DynamoDbClient dynamoDb, String tableName) {
        try {
            // Try to describe - if exists, log and return
            dynamoDb.describeTable(DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build());
            
            log.info("✓ Table '{}' already exists", tableName);
            return;
            
        } catch (ResourceNotFoundException e) {
            // Table doesn't exist, create it
            log.info("→ Creating table '{}'...", tableName);
            
            try {
                createTable(dynamoDb, tableName);
                log.info("✓ Table '{}' created successfully", tableName);
                
                // Wait for table to be active (max 5 minutes)
                waitForTableActive(dynamoDb, tableName);
                log.info("✓ Table '{}' is now ACTIVE", tableName);
                
            } catch (Exception createError) {
                log.error("✗ Failed to create table '{}': {}", tableName, createError.getMessage());
            }
        } catch (Exception e) {
            log.error("✗ Error checking table '{}': {}", tableName, e.getMessage());
        }
    }

    /**
     * Creates individual DynamoDB tables with proper schema
     * 
     * TABLE SCHEMAS:
     * 
     * 1. WALLET TABLE
     *    PK: userId (String)
     *    Attributes: balance, pendingPayout, totalWithdrawn
     *    GSI: walletType-index for wallet type queries
     * 
     * 2. WALLET_TRANSACTION TABLE
     *    PK: transactionId (String)
     *    SK: createdAt (Number)
     *    GSI: userId-createdAt for trainer queries
     * 
     * 3. PAYMENT TABLE
     *    PK: paymentId (String)
     *    Attributes: userId, courseId, amount, status
     *    GSI: userId-status for payment queries
     */
    private void createTable(DynamoDbClient dynamoDb, String tableName) {
        switch (tableName) {
            case "Wallet":
                createWalletTable(dynamoDb);
                break;
            case "WalletTransaction":
                createWalletTransactionTable(dynamoDb);
                break;
            case "Payment":
                createPaymentTable(dynamoDb);
                break;
            default:
                log.warn("Unknown table: {}", tableName);
        }
    }

    /**
     * Creates Wallet table with schema:
     * 
     * PARTITION KEY: userId
     * SORT KEY: none (single item per user)
     * 
     * ATTRIBUTES:
     * - userId (String) - PK
     * - balance (Number) - current balance
     * - pendingPayout (Number) - pending withdrawal
     * - totalWithdrawn (Number) - total withdrawn
     * - walletType (String) - TRAINER or USER
     * - createdAt (Number) - creation timestamp
     * - updatedAt (Number) - last update timestamp
     */
    private void createWalletTable(DynamoDbClient dynamoDb) {
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName("Wallet")
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("userId")
                                .keyType(KeyType.HASH)
                                .build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("userId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("walletType")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("walletType-index")
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName("walletType")
                                                .keyType(KeyType.HASH)
                                                .build()
                                )
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.ALL)
                                        .build())
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                        .readCapacityUnits(5L)
                                        .writeCapacityUnits(5L)
                                        .build())
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        dynamoDb.createTable(request);
    }

    /**
     * Creates WalletTransaction table with schema:
     * 
     * PARTITION KEY: transactionId (unique per transaction)
     * SORT KEY: createdAt (for time-series queries)
     * 
     * ATTRIBUTES:
     * - transactionId (String) - PK
     * - createdAt (Number) - SK, timestamp
     * - userId (String) - transaction owner
     * - amount (Number) - transaction amount
     * - type (String) - CREDIT, DEBIT, WITHDRAW, PAYOUT_REQUEST
     * - status (String) - PENDING, COMPLETED, FAILED
     * 
     * GSI: userId-createdAt for querying trainer's transactions
     */
    private void createWalletTransactionTable(DynamoDbClient dynamoDb) {
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName("WalletTransaction")
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("transactionId")
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName("createdAt")
                                .keyType(KeyType.RANGE)
                                .build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("transactionId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("createdAt")
                                .attributeType(ScalarAttributeType.N)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("userId")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("userId-createdAt-index")
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName("userId")
                                                .keyType(KeyType.HASH)
                                                .build(),
                                        KeySchemaElement.builder()
                                                .attributeName("createdAt")
                                                .keyType(KeyType.RANGE)
                                                .build()
                                )
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.ALL)
                                        .build())
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                        .readCapacityUnits(5L)
                                        .writeCapacityUnits(5L)
                                        .build())
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        dynamoDb.createTable(request);
    }

    /**
     * Creates Payment table with schema:
     * 
     * PARTITION KEY: paymentId (unique per payment)
     * SORT KEY: createdAt (for time-series queries)
     * 
     * ATTRIBUTES:
     * - paymentId (String) - PK
     * - createdAt (Number) - SK, timestamp
     * - userId (String) - buyer
     * - courseId (String) - purchased course
     * - amount (Number) - payment amount
     * - status (String) - COMPLETED, FAILED, REFUNDED
     * 
     * GSI: userId-status for querying user payments
     */
    private void createPaymentTable(DynamoDbClient dynamoDb) {
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName("Payment")
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("paymentId")
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName("createdAt")
                                .keyType(KeyType.RANGE)
                                .build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("paymentId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("createdAt")
                                .attributeType(ScalarAttributeType.N)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("userId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("status")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("userId-status-index")
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName("userId")
                                                .keyType(KeyType.HASH)
                                                .build(),
                                        KeySchemaElement.builder()
                                                .attributeName("status")
                                                .keyType(KeyType.RANGE)
                                                .build()
                                )
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.ALL)
                                        .build())
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                        .readCapacityUnits(5L)
                                        .writeCapacityUnits(5L)
                                        .build())
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        dynamoDb.createTable(request);
    }

    /**
     * Waits for a table to reach ACTIVE status
     * 
     * POLLING LOGIC:
     * - Polls every 2 seconds
     * - Max wait time: 5 minutes
     * - Returns when ACTIVE or timeout
     * 
     * PARAMETERS:
     * - dynamoDb: DynamoDbClient
     * - tableName: Table to check
     */
    private void waitForTableActive(DynamoDbClient dynamoDb, String tableName) {
        long maxWaitTime = 5 * 60 * 1000; // 5 minutes
        long startTime = System.currentTimeMillis();
        long pollInterval = 2000; // 2 seconds

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            try {
                DescribeTableResponse response = dynamoDb.describeTable(
                        DescribeTableRequest.builder()
                                .tableName(tableName)
                                .build()
                );

                if (response.table().tableStatus() == TableStatus.ACTIVE) {
                    return; // Table is active
                }

                log.info("Waiting for table '{}' to be ACTIVE (current: {})", 
                        tableName, response.table().tableStatus());
                
                Thread.sleep(pollInterval);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for table: " + tableName, e);
            } catch (Exception e) {
                log.warn("Error checking table status: {}", e.getMessage());
            }
        }

        log.warn("Table '{}' did not become ACTIVE within timeout period", tableName);
    }
}