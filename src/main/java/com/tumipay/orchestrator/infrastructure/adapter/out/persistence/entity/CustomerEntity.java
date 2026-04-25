package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad JPA que representa los datos de un cliente en la base de datos.
 * Almacena la información personal del cliente asociado a una transacción.
 */
@Entity
@Table(name = "customers")
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
