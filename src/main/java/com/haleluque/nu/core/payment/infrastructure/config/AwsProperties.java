package com.haleluque.nu.core.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS properties (DynamoDB, region) for application.yml.
 */
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    private String region;
    private Dynamodb dynamodb = new Dynamodb();

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Dynamodb getDynamodb() {
        return dynamodb;
    }

    public void setDynamodb(Dynamodb dynamodb) {
        this.dynamodb = dynamodb;
    }

    public static class Dynamodb {
        private String endpoint;
        private String accessKeyId;
        private String secretAccessKey;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }
    }
}
