package com.haleluque.nu.core.payment.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Creates DynamoDB tables on startup when a custom endpoint is configured (e.g. LocalStack).
 * Retries with backoff so the service can start before LocalStack is ready.
 */
@Component
@Order(1)
public class DynamoDbTableInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTableInitializer.class);
    private static final String ACCOUNTS_TABLE = "nu-core-payment-accounts";
    private static final String TRANSACTIONS_TABLE = "nu-core-payment-transactions";
    private static final int MAX_ATTEMPTS = 30;
    private static final long DELAY_MS = 2_000;

    private final DynamoDbClient dynamoDbClient;
    private final AwsProperties awsProperties;

    public DynamoDbTableInitializer(DynamoDbClient dynamoDbClient, AwsProperties awsProperties) {
        this.dynamoDbClient = dynamoDbClient;
        this.awsProperties = awsProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String endpoint = awsProperties.getDynamodb() != null ? awsProperties.getDynamodb().getEndpoint() : null;
        if (endpoint == null || endpoint.isBlank()) {
            log.debug("No DynamoDB endpoint set; skipping table creation.");
            return;
        }

        log.info("DynamoDB endpoint set ({}); ensuring tables exist.", endpoint);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ensureTable(ACCOUNTS_TABLE, "id");
                ensureTable(TRANSACTIONS_TABLE, "id");
                log.info("DynamoDB tables ready: {}, {}", ACCOUNTS_TABLE, TRANSACTIONS_TABLE);
                return;
            } catch (Exception e) {
                log.warn("Attempt {}/{} - DynamoDB not ready: {}. Retrying in {}ms.",
                        attempt, MAX_ATTEMPTS, e.getMessage(), DELAY_MS);
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Could not create DynamoDB tables after {} attempts. Seed and transfer may fail.", MAX_ATTEMPTS);
                    return;
                }
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Table initializer interrupted.");
                    return;
                }
            }
        }
    }

    private void ensureTable(String tableName, String keyAttribute) {
        if (tableExists(tableName)) {
            return;
        }
        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(keyAttribute)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(keyAttribute)
                        .keyType(KeyType.HASH)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
        log.info("Created table: {}", tableName);
    }

    private boolean tableExists(String tableName) {
        try {
            dynamoDbClient.describeTable(b -> b.tableName(tableName));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
