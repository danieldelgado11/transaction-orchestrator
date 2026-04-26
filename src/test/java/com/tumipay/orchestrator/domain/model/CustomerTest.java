package com.tumipay.orchestrator.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para el modelo de dominio Customer.
 * Verifica la construcción de clientes usando el patrón Builder
 * con campos obligatorios y opcionales.
 */
class CustomerTest {

    @Test
    @DisplayName("Debe crear cliente con todos los campos usando builder")
    void builder_shouldCreateCustomerWithAllFields() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(id)
                .documentType("CC")
                .documentNumber("1234567890")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("juan@example.com")
                .firstName("Juan")
                .middleName("Carlos")
                .lastName("Perez")
                .secondLastName("Gomez")
                .build();

        assertThat(customer.getId()).isEqualTo(id);
        assertThat(customer.getDocumentType()).isEqualTo("CC");
        assertThat(customer.getDocumentNumber()).isEqualTo("1234567890");
        assertThat(customer.getCountryCallingCode()).isEqualTo("+57");
        assertThat(customer.getPhoneNumber()).isEqualTo("3001234567");
        assertThat(customer.getEmail()).isEqualTo("juan@example.com");
        assertThat(customer.getFirstName()).isEqualTo("Juan");
        assertThat(customer.getMiddleName()).isEqualTo("Carlos");
        assertThat(customer.getLastName()).isEqualTo("Perez");
        assertThat(customer.getSecondLastName()).isEqualTo("Gomez");
    }

    @Test
    @DisplayName("Debe crear cliente con solo los campos requeridos")
    void builder_shouldCreateCustomerWithRequiredFields() {
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        assertThat(customer.getMiddleName()).isNull();
        assertThat(customer.getSecondLastName()).isNull();
    }

    @Test
    @DisplayName("Debe crear cliente con id null para nueva entidad")
    void builder_shouldAllowNullId() {
        Customer customer = Customer.builder()
                .id(null)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        assertThat(customer.getId()).isNull();
    }
}
