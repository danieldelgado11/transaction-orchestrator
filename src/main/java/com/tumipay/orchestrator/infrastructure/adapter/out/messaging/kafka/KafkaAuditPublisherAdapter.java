package com.tumipay.orchestrator.infrastructure.adapter.out.messaging.kafka;

import com.tumipay.orchestrator.domain.model.CustomerAuditEvent;
import com.tumipay.orchestrator.domain.model.TransactionAuditEvent;
import com.tumipay.orchestrator.domain.port.out.AuditPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Adaptador de salida que implementa la publicación de eventos de auditoría usando Kafka.
 * Desacopla el dominio de la infraestructura de mensajería.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAuditPublisherAdapter implements AuditPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String LOG_PUBLISH_ERROR = "Error al publicar evento de auditoría";

    @Value("${kafka.topics.customer-audit}")
    private String customerAuditTopic;

    @Value("${kafka.topics.transaction-audit}")
    private String transactionAuditTopic;

    @Override
    public void publishCustomerAudit(CustomerAuditEvent event) {
        Objects.requireNonNull(event, "CustomerAuditEvent no puede ser null");
        log.info("Publicando evento de auditoría de cliente: customerId={}, action={}",
                event.getCustomerId(), event.getAction());
        publishEvent(customerAuditTopic, event.getCustomerId().toString(), event, "cliente");
    }

    @Override
    public void publishTransactionAudit(TransactionAuditEvent event) {
        Objects.requireNonNull(event, "TransactionAuditEvent no puede ser null");
        log.info("Publicando evento de auditoría de transacción: transactionId={}, action={}, status={}",
                event.getTransactionId(), event.getAction(), event.getStatus());
        publishEvent(transactionAuditTopic, event.getTransactionId().toString(), event, "transacción");
    }

    private void publishEvent(String topic, String key, Object event, String entityType) {
        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> handlePublishResult(result, ex, entityType));
        } catch (KafkaException e) {
            log.error("{} de {}: {}", LOG_PUBLISH_ERROR, entityType, e.getMessage(), e);
        }
    }

    private void handlePublishResult(org.springframework.kafka.support.SendResult<String, Object> result,
                                     Throwable ex, String entityType) {
        if (ex != null) {
            log.error("{} de {}: {}", LOG_PUBLISH_ERROR, entityType, ex.getMessage(), ex);
        } else {
            log.debug("Evento de auditoría de {} publicado: partition={}, offset={}",
                    entityType,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        }
    }
}
