package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA — solo preocupación de persistencia. Separada deliberadamente del modelo de dominio
 * para mantener el dominio libre de anotaciones de framework (principio de Arquitectura Hexagonal).
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_client_transaction_id", columnList = "client_transaction_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "client_transaction_id", nullable = false, unique = true, length = 100)
    private String clientTransactionId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "payment_method_id", nullable = false, length = 50)
    private String paymentMethodId;

    @Column(name = "webhook_url", nullable = false, length = 500)
    private String webhookUrl;

    @Column(name = "redirect_url", nullable = false, length = 500)
    private String redirectUrl;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "expiration_seconds")
    private Long expirationSeconds;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private CustomerEntity customer;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
