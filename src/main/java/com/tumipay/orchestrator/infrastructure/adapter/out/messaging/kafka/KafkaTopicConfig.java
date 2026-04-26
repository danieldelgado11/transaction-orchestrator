package com.tumipay.orchestrator.infrastructure.adapter.out.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuración de topics Kafka para auditoría.
 * Crea automáticamente los topics necesarios si no existen.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.customer-audit}")
    private String customerAuditTopic;

    @Value("${kafka.topics.transaction-audit}")
    private String transactionAuditTopic;

    @Bean
    public NewTopic customerAuditTopic() {
        return TopicBuilder.name(customerAuditTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionAuditTopic() {
        return TopicBuilder.name(transactionAuditTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
