package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad JPA que representa un cliente en la base de datos.
 *
 * Decision: Customer es una Entity con identidad propia. Un cliente puede tener
 * múltiples transacciones. Se busca/crea por documento (tipo+numero) o email
 * para mantener idempotencia en la creación de clientes.
 */
@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(name = "idx_customers_email", columnList = "email", unique = true),
        @Index(name = "idx_customers_document", columnList = "document_type, document_number", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "document_type", nullable = false, length = 20)
    private String documentType;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "country_calling_code", nullable = false, length = 6)
    private String countryCallingCode;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "second_last_name", length = 100)
    private String secondLastName;
}
