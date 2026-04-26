package com.tumipay.orchestrator.infrastructure.adapter.out.persistence;

import com.tumipay.orchestrator.domain.model.CustomerAuditEvent;
import com.tumipay.orchestrator.domain.model.TransactionAuditEvent;
import com.tumipay.orchestrator.domain.port.out.AuditRepositoryPort;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.CustomerAuditEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.TransactionAuditEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.CustomerAuditJpaRepository;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.TransactionAuditJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Adaptador de salida que implementa la persistencia de auditoría.
 * Convierte eventos de dominio en entidades JPA y las almacena en la base de datos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditPersistenceAdapter implements AuditRepositoryPort {

    private final CustomerAuditJpaRepository customerAuditRepository;
    private final TransactionAuditJpaRepository transactionAuditRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCustomerAudit(CustomerAuditEvent event) {
        Objects.requireNonNull(event, "CustomerAuditEvent no puede ser null");
        log.debug("Guardando auditoría de cliente: customerId={}, action={}",
                event.getCustomerId(), event.getAction());
        
        CustomerAuditEntity entity = CustomerAuditEntity.builder()
                .customerId(event.getCustomerId())
                .action(event.getAction().name())
                .documentType(event.getDocumentType())
                .documentNumber(event.getDocumentNumber())
                .countryCallingCode(event.getCountryCallingCode())
                .phoneNumber(event.getPhoneNumber())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .middleName(event.getMiddleName())
                .lastName(event.getLastName())
                .secondLastName(event.getSecondLastName())
                .changedBy(event.getChangedBy())
                .changedAt(event.getChangedAt())
                .transactionId(event.getTransactionId())
                .build();

        customerAuditRepository.save(entity);
        log.debug("Auditoría de cliente guardada exitosamente: id={}", entity.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveTransactionAudit(TransactionAuditEvent event) {
        Objects.requireNonNull(event, "TransactionAuditEvent no puede ser null");
        log.debug("Guardando auditoría de transacción: transactionId={}, action={}, status={}",
                event.getTransactionId(), event.getAction(), event.getStatus());
        
        TransactionAuditEntity entity = TransactionAuditEntity.builder()
                .transactionId(event.getTransactionId())
                .action(event.getAction().name())
                .clientTransactionId(event.getClientTransactionId())
                .amountCents(event.getAmountCents())
                .currencyCode(event.getCurrencyCode())
                .countryCode(event.getCountryCode())
                .paymentMethodId(event.getPaymentMethodId())
                .webhookUrl(event.getWebhookUrl())
                .redirectUrl(event.getRedirectUrl())
                .description(event.getDescription())
                .expirationSeconds(event.getExpirationSeconds())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .oldStatus(event.getOldStatus() != null ? event.getOldStatus().name() : null)
                .processedAt(event.getProcessedAt())
                .customerId(event.getCustomerId())
                .changedBy(event.getChangedBy())
                .changedAt(event.getChangedAt())
                .build();

        transactionAuditRepository.save(entity);
        log.debug("Auditoría de transacción guardada exitosamente: id={}", entity.getId());
    }
}
