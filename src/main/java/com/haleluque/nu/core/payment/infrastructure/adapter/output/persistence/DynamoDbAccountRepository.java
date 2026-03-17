package com.haleluque.nu.core.payment.infrastructure.adapter.output.persistence;

import com.haleluque.nu.core.payment.application.port.out.AccountRepository;
import com.haleluque.nu.core.payment.domain.model.Account;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Output adapter: DynamoDB implementation of AccountRepository.
 */
public class DynamoDbAccountRepository implements AccountRepository {

    private static final String TABLE_NAME = "nu-core-payment-accounts";
    private static final String PK_ID = "id";
    private static final String ATTR_CUSTOMER_NAME = "customerName";
    private static final String ATTR_BALANCE = "balance";

    private final DynamoDbClient dynamoDbClient;

    public DynamoDbAccountRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(PK_ID, AttributeValue.builder().s(id.toString()).build()))
                .build();

        var response = dynamoDbClient.getItem(request);
        if (response.item() == null || response.item().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapToAccount(response.item()));
    }

    @Override
    public List<Account> findAll() {
        var response = dynamoDbClient.scan(ScanRequest.builder().tableName(TABLE_NAME).build());
        return response.items().stream()
                .map(DynamoDbAccountRepository::mapToAccount)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Account account) {
        Map<String, AttributeValue> item = Map.of(
                PK_ID, AttributeValue.builder().s(account.id().toString()).build(),
                ATTR_CUSTOMER_NAME, AttributeValue.builder().s(account.customerName()).build(),
                ATTR_BALANCE, AttributeValue.builder().n(account.balance().toPlainString()).build()
        );
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build());
    }

    private static Account mapToAccount(Map<String, AttributeValue> item) {
        UUID id = UUID.fromString(item.get(PK_ID).s());
        String customerName = item.get(ATTR_CUSTOMER_NAME).s();
        BigDecimal balance = new BigDecimal(item.get(ATTR_BALANCE).n());
        return new Account(id, customerName, balance);
    }
}
