package com.haleluque.nu.core.payment.infrastructure.adapter.output.persistence;

import com.haleluque.nu.core.payment.application.port.out.TransactionRepository;
import com.haleluque.nu.core.payment.domain.model.Transaction;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Output adapter: transaction persistence in DynamoDB.
 */
public class DynamoDbTransactionRepository implements TransactionRepository {

    private static final String TABLE_NAME = "nu-core-payment-transactions";
    private static final String PK_ID = "id";
    private static final String ATTR_ACCOUNT_ID = "accountId";
    private static final String ATTR_DESTINATION_ACCOUNT_ID = "destinationAccountId";
    private static final String ATTR_AMOUNT = "amount";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_STATUS = "status";
    private static final String ATTR_CREATED_AT = "createdAt";

    private final DynamoDbClient dynamoDbClient;

    public DynamoDbTransactionRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(PK_ID, AttributeValue.builder().s(id.toString()).build()))
                .build();

        var response = dynamoDbClient.getItem(request);
        if (response.item() == null || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapToTransaction(response.item()));
    }

    @Override
    public void save(Transaction transaction) {
        var itemBuilder = new java.util.HashMap<String, AttributeValue>();
        itemBuilder.put(PK_ID, AttributeValue.builder().s(transaction.id().toString()).build());
        itemBuilder.put(ATTR_ACCOUNT_ID, AttributeValue.builder().s(transaction.accountId().toString()).build());
        if (transaction.destinationAccountId() != null) {
            itemBuilder.put(ATTR_DESTINATION_ACCOUNT_ID, AttributeValue.builder().s(transaction.destinationAccountId().toString()).build());
        }
        itemBuilder.put(ATTR_AMOUNT, AttributeValue.builder().n(transaction.amount().toPlainString()).build());
        itemBuilder.put(ATTR_TYPE, AttributeValue.builder().s(transaction.type()).build());
        itemBuilder.put(ATTR_STATUS, AttributeValue.builder().s(transaction.status()).build());
        itemBuilder.put(ATTR_CREATED_AT, AttributeValue.builder().s(transaction.createdAt().toString()).build());
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(itemBuilder)
                .build());
    }

    private static Transaction mapToTransaction(Map<String, AttributeValue> item) {
        UUID id = UUID.fromString(item.get(PK_ID).s());
        UUID accountId = UUID.fromString(item.get(ATTR_ACCOUNT_ID).s());
        UUID destinationAccountId = item.containsKey(ATTR_DESTINATION_ACCOUNT_ID) && item.get(ATTR_DESTINATION_ACCOUNT_ID).s() != null
                ? UUID.fromString(item.get(ATTR_DESTINATION_ACCOUNT_ID).s())
                : null;
        BigDecimal amount = new BigDecimal(item.get(ATTR_AMOUNT).n());
        String type = item.get(ATTR_TYPE).s();
        String status = item.get(ATTR_STATUS).s();
        Instant createdAt = Instant.parse(item.get(ATTR_CREATED_AT).s());
        return new Transaction(id, accountId, destinationAccountId, amount, type, status, createdAt);
    }
}
