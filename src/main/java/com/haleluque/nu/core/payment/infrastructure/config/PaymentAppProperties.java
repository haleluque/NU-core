package com.haleluque.nu.core.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration for payment module (Kafka topics, metric names).
 */
@ConfigurationProperties(prefix = "app.payment")
public class PaymentAppProperties {

    private Kafka kafka = new Kafka();
    private Metrics metrics = new Metrics();

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public static class Kafka {
        private String topicPaymentEvents;
        private String topicRiskEvents;
        private String topicPaymentCompleted;

        public String getTopicPaymentEvents() {
            return topicPaymentEvents;
        }

        public void setTopicPaymentEvents(String topicPaymentEvents) {
            this.topicPaymentEvents = topicPaymentEvents;
        }

        public String getTopicRiskEvents() {
            return topicRiskEvents;
        }

        public void setTopicRiskEvents(String topicRiskEvents) {
            this.topicRiskEvents = topicRiskEvents;
        }

        public String getTopicPaymentCompleted() {
            return topicPaymentCompleted;
        }

        public void setTopicPaymentCompleted(String topicPaymentCompleted) {
            this.topicPaymentCompleted = topicPaymentCompleted;
        }
    }

    public static class Metrics {
        private String paymentsProcessedTotal;
        private String paymentsCompensatedTotal;

        public String getPaymentsProcessedTotal() {
            return paymentsProcessedTotal;
        }

        public void setPaymentsProcessedTotal(String paymentsProcessedTotal) {
            this.paymentsProcessedTotal = paymentsProcessedTotal;
        }

        public String getPaymentsCompensatedTotal() {
            return paymentsCompensatedTotal;
        }

        public void setPaymentsCompensatedTotal(String paymentsCompensatedTotal) {
            this.paymentsCompensatedTotal = paymentsCompensatedTotal;
        }
    }
}
