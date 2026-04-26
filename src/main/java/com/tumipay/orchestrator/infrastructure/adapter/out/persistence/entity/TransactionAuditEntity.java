package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para auditoría de cambios en transacciones.
 * Almacena un registro histórico de todas las operaciones realizadas sobre transactions.
 */
@Entity
@Table(name = "transaction_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "action", nullable = false, length = 10)
    private String action;

    @Column(name = "client_transaction_id", length = 100)
    private String clientTransactionId;

    @Column(name = "amount_cents")
    private Long amountCents;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "payment_method_id", length = 50)
    private String paymentMethodId;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "redirect_url", length = 500)
    private String redirectUrl;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "expiration_seconds")
    private Long expirationSeconds;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;
}
