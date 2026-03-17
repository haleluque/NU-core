package com.haleluque.nu.core.payment;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.URI;
import java.util.Map;

/**
 * Base class for integration tests. Uses Singleton Container pattern: one Kafka and one LocalStack
 * (DynamoDB) shared across all tests. Properties are injected via {@link DynamicPropertySource}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static final String ACCOUNTS_TABLE = "nu-core-payment-accounts";
    private static final String TRANSACTIONS_TABLE = "nu-core-payment-transactions";

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers/JUnit, not manually closed here.
    protected static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.2"))
            .withReuse(true);

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers/JUnit, not manually closed here.
    protected static final LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0")
                    .asCompatibleSubstituteFor("localstack/localstack"))
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withReuse(true);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("aws.dynamodb.endpoint", () -> localStack.getEndpoint().toString());
        registry.add("aws.dynamodb.access-key-id", localStack::getAccessKey);
        registry.add("aws.dynamodb.secret-access-key", localStack::getSecretKey);
        registry.add("aws.region", () -> localStack.getRegion());
    }

    @org.junit.jupiter.api.BeforeAll
    static void createDynamoDbTables() {
        String endpoint = localStack.getEndpoint().toString();
        try (DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .build()) {

            if (!tableExists(client, ACCOUNTS_TABLE)) {
                client.createTable(CreateTableRequest.builder()
                        .tableName(ACCOUNTS_TABLE)
                        .attributeDefinitions(AttributeDefinition.builder()
                                .attributeName("id")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("id")
                                .keyType(KeyType.HASH)
                                .build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build());
            }
            if (!tableExists(client, TRANSACTIONS_TABLE)) {
                client.createTable(CreateTableRequest.builder()
                        .tableName(TRANSACTIONS_TABLE)
                        .attributeDefinitions(AttributeDefinition.builder()
                                .attributeName("id")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("id")
                                .keyType(KeyType.HASH)
                                .build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build());
            }

            seedAccounts(client);
        }
    }

    private static void seedAccounts(DynamoDbClient client) {
        putAccount(client, "11111111-1111-1111-1111-111111111111", "Alice", "5000.00");
        putAccount(client, "22222222-2222-2222-2222-222222222222", "Bob", "3000.50");
    }

    private static void putAccount(DynamoDbClient client, String id, String customerName, String balance) {
        client.putItem(PutItemRequest.builder()
                .tableName(ACCOUNTS_TABLE)
                .item(Map.of(
                        "id", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(id).build(),
                        "customerName", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(customerName).build(),
                        "balance", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().n(balance).build()))
                .build());
    }

    private static boolean tableExists(DynamoDbClient client, String tableName) {
        try {
            client.describeTable(b -> b.tableName(tableName));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
