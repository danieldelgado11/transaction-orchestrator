package com.tumipay.orchestrator.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Raíz agregada de Transacción - entidad de dominio central.
 * Encapsula todas las reglas de negocio relacionadas con una transacción de pago.
 */
@Getter
@Builder
public class Transaction {

    private final UUID id;
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
    private final Customer customer;
    private final LocalDateTime processedAt;
    private final LocalDateTime createdAt;

    public static Transaction create(
            String clientTransactionId,
            Long amountCents,
            String currencyCode,
            String countryCode,
            String paymentMethodId,
            String webhookUrl,
            String redirectUrl,
            String description,
            Long expirationSeconds,
            Customer customer) {

        return Transaction.builder()
                .id(UUID.randomUUID())
                .clientTransactionId(clientTransactionId)
                .amountCents(amountCents)
                .currencyCode(currencyCode.trim().toUpperCase())
                .countryCode(countryCode.trim().toUpperCase())
                .paymentMethodId(paymentMethodId)
                .webhookUrl(webhookUrl)
                .redirectUrl(redirectUrl)
                .description(description)
                .expirationSeconds(expirationSeconds)
                .status(TransactionStatus.PENDING)
                .customer(customer)
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public Transaction withStatus(TransactionStatus newStatus) {
        return Transaction.builder()
                .id(this.id)
                .clientTransactionId(this.clientTransactionId)
                .amountCents(this.amountCents)
                .currencyCode(this.currencyCode)
                .countryCode(this.countryCode)
                .paymentMethodId(this.paymentMethodId)
                .webhookUrl(this.webhookUrl)
                .redirectUrl(this.redirectUrl)
                .description(this.description)
                .expirationSeconds(this.expirationSeconds)
                .status(newStatus)
                .customer(this.customer)
                .processedAt(this.processedAt)
                .createdAt(this.createdAt)
                .build();
    }
}
