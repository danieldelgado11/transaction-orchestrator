package com.tumipay.orchestrator.infrastructure.adapter.out.messaging.kafka;

import com.tumipay.orchestrator.domain.model.CustomerAuditEvent;
import com.tumipay.orchestrator.domain.model.TransactionAuditEvent;
import com.tumipay.orchestrator.domain.port.out.AuditRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Consumidor Kafka que procesa eventos de auditoría y los persiste en la base de datos.
 * Opera de forma asíncrona y desacoplada de la transacción principal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAuditConsumer {

    private final AuditRepositoryPort auditRepository;

    @KafkaListener(
            topics = "${kafka.topics.customer-audit}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCustomerAuditEvent(
            @Payload CustomerAuditEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        Objects.requireNonNull(event, "CustomerAuditEvent no puede ser null");
        log.info("Consumiendo evento de auditoría de cliente [topic={}, offset={}]: customerId={}, action={}",
                topic, offset, event.getCustomerId(), event.getAction());
        try {
            auditRepository.saveCustomerAudit(event);
            log.debug("Evento de auditoría de cliente guardado: customerId={}", event.getCustomerId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Violación de integridad al guardar auditoría de cliente: {}", e.getMessage());
        } catch (DataAccessException e) {
            log.error("Error de base de datos al procesar auditoría de cliente: {}", e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Error inesperado al procesar auditoría de cliente: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.transaction-audit}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionAuditEvent(
            @Payload TransactionAuditEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        Objects.requireNonNull(event, "TransactionAuditEvent no puede ser null");
        log.info("Consumiendo evento de auditoría de transacción [topic={}, offset={}]: transactionId={}, action={}",
                topic, offset, event.getTransactionId(), event.getAction());
        try {
            auditRepository.saveTransactionAudit(event);
            log.debug("Evento de auditoría de transacción guardado: transactionId={}", event.getTransactionId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Violación de integridad al guardar auditoría de transacción (transacción aún no existe): transactionId={}",
                    event.getTransactionId());
        } catch (DataAccessException e) {
            log.error("Error de base de datos al procesar auditoría de transacción: {}", e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Error inesperado al procesar auditoría de transacción: {}", e.getMessage(), e);
        }
    }
}
