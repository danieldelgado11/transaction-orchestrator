package com.tumipay.orchestrator.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Entidad Cliente - tiene identidad propia.
 *
 * Un cliente puede tener múltiples transacciones. Se identifica por documento
 * (tipo + número) o email para evitar duplicados.
 */
@Getter
@Builder
public class Customer {

    private final UUID id;
    private final String documentType;
    private final String documentNumber;
    private final String countryCallingCode;
    private final String phoneNumber;
    private final String email;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String secondLastName;
}
