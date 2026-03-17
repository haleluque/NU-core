package com.haleluque.nu.core.payment.infrastructure.config;

import com.haleluque.nu.core.payment.application.port.out.AccountRepository;
import com.haleluque.nu.core.payment.application.port.out.TransactionRepository;
import com.haleluque.nu.core.payment.infrastructure.adapter.output.persistence.DynamoDbAccountRepository;
import com.haleluque.nu.core.payment.infrastructure.adapter.output.persistence.DynamoDbTransactionRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.utils.AttributeMap;

import java.net.URI;

/**
 * DynamoDB client and persistence adapter configuration.
 */
@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(AwsProperties aws) {
        String region = aws.getRegion() != null ? aws.getRegion() : "us-east-1";
        String endpoint = aws.getDynamodb() != null && aws.getDynamodb().getEndpoint() != null
                ? aws.getDynamodb().getEndpoint()
                : "";
        var builder = DynamoDbClient.builder()
                .region(Region.of(region));
        if (!endpoint.isBlank()) {
            var creds = aws.getDynamodb();
            String accessKey = creds.getAccessKeyId() != null ? creds.getAccessKeyId() : "test";
            String secretKey = creds.getSecretAccessKey() != null ? creds.getSecretAccessKey() : "test";
            // LocalStack/development: trust self-signed HTTPS (Apache client honors TRUST_ALL_CERTIFICATES)
            var httpClient = ApacheHttpClient.builder()
                    .buildWithDefaults(AttributeMap.builder()
                            .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                            .build());
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                    .httpClient(httpClient);
        }
        return builder.build();
    }

    @Bean
    public AccountRepository accountRepository(DynamoDbClient dynamoDbClient) {
        return new DynamoDbAccountRepository(dynamoDbClient);
    }

    @Bean
    public TransactionRepository transactionRepository(DynamoDbClient dynamoDbClient) {
        return new DynamoDbTransactionRepository(dynamoDbClient);
    }
}
