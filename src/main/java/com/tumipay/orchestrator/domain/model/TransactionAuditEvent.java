package com.tumipay.orchestrator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de dominio que representa una operación de auditoría sobre una transacción.
 * Este evento se publica en Kafka para ser procesado asíncronamente.
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class TransactionAuditEvent {

    private final UUID transactionId;
    private final AuditAction action;
    private final String clientTransactionId;
    private final Long amountCents;
    private final String currencyCode;
    private final String countryCode;
    private final String paymentMethodId;
    private final String webhookUrl;
    private final String redirectUrl;
    private final String description;
    private final Long expirationSeconds;
    private final TransactionStatus status;
    private final TransactionStatus oldStatus;
    private final LocalDateTime processedAt;
    private final UUID customerId;
    private final String changedBy;
    private final LocalDateTime changedAt;

    public static TransactionAuditEvent fromTransaction(Transaction transaction, AuditAction action, String changedBy) {
        return TransactionAuditEvent.builder()
                .transactionId(transaction.getId())
                .action(action)
                .clientTransactionId(transaction.getClientTransactionId())
                .amountCents(transaction.getAmountCents())
                .currencyCode(transaction.getCurrencyCode())
                .countryCode(transaction.getCountryCode())
                .paymentMethodId(transaction.getPaymentMethodId())
                .webhookUrl(transaction.getWebhookUrl())
                .redirectUrl(transaction.getRedirectUrl())
                .description(transaction.getDescription())
                .expirationSeconds(transaction.getExpirationSeconds())
                .status(transaction.getStatus())
                .processedAt(transaction.getProcessedAt())
                .customerId(transaction.getCustomer() != null ? transaction.getCustomer().getId() : null)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
    }

    public static TransactionAuditEvent fromTransaction(Transaction transaction, AuditAction action, String changedBy, TransactionStatus oldStatus) {
        TransactionAuditEvent event = fromTransaction(transaction, action, changedBy);
        return event.toBuilder()
                .oldStatus(oldStatus)
                .build();
    }
}
