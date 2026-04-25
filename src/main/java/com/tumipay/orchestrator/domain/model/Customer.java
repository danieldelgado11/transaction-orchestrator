package com.tumipay.orchestrator.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Objeto de valor Cliente - inmutable por diseño.
 */
@Getter
@Builder
public class Customer {

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
