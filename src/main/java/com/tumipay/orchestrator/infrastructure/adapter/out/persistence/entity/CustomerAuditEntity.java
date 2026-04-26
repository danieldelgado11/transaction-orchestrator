package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para auditoría de cambios en clientes.
 * Almacena un registro histórico de todas las operaciones realizadas sobre customer.
 */
@Entity
@Table(name = "customer_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "action", nullable = false, length = 10)
    private String action;

    @Column(name = "document_type", length = 20)
    private String documentType;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Column(name = "country_calling_code", length = 6)
    private String countryCallingCode;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "second_last_name", length = 100)
    private String secondLastName;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "transaction_id")
    private UUID transactionId;
}
