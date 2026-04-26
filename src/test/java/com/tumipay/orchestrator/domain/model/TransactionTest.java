package com.tumipay.orchestrator.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para el modelo de dominio Transaction.
 * Verifica la creación de transacciones, normalización de códigos
 * y el patrón inmutable withStatus.
 */
class TransactionTest {

    @Test
    @DisplayName("Debe crear transacción con todos los campos y estado PENDING por defecto")
    void create_shouldSetDefaults() {
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

        Transaction tx = Transaction.create(
                "CLIENT-001",
                100000L,
                "cop",
                "co",
                "MOCK_PSP",
                "https://webhook.com",
                "https://return.com",
                "Test desc",
                1800L,
                customer
        );

        assertThat(tx.getId()).isNotNull();
        assertThat(tx.getClientTransactionId()).isEqualTo("CLIENT-001");
        assertThat(tx.getAmountCents()).isEqualTo(100000L);
        assertThat(tx.getCurrencyCode()).isEqualTo("COP"); // normalized
        assertThat(tx.getCountryCode()).isEqualTo("CO"); // normalized
        assertThat(tx.getPaymentMethodId()).isEqualTo("MOCK_PSP");
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(tx.getProcessedAt()).isNotNull();
        assertThat(tx.getCreatedAt()).isNotNull();
        assertThat(tx.getCustomer()).isEqualTo(customer);
    }

    @Test
    @DisplayName("Debe normalizar códigos nulos a null")
    void create_withNullCodes_shouldKeepNull() {
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

        Transaction tx = Transaction.create(
                "CLIENT-001",
                100000L,
                null,
                null,
                "MOCK_PSP",
                "https://webhook.com",
                "https://return.com",
                null,
                null,
                customer
        );

        assertThat(tx.getCurrencyCode()).isNull();
        assertThat(tx.getCountryCode()).isNull();
        assertThat(tx.getDescription()).isNull();
        assertThat(tx.getExpirationSeconds()).isNull();
    }

    @Test
    @DisplayName("Debe recortar y convertir a mayúsculas los códigos de moneda y país")
    void create_shouldNormalizeCodes() {
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

        Transaction tx = Transaction.create(
                "CLIENT-001",
                100000L,
                "  cop  ",
                "  co  ",
                "MOCK_PSP",
                "https://webhook.com",
                "https://return.com",
                "desc",
                1800L,
                customer
        );

        assertThat(tx.getCurrencyCode()).isEqualTo("COP");
        assertThat(tx.getCountryCode()).isEqualTo("CO");
    }

    @Test
    @DisplayName("Debe crear nueva transacción con estado actualizado vía withStatus")
    void withStatus_shouldReturnNewTransactionWithUpdatedStatus() {
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

        Transaction original = Transaction.create(
                "CLIENT-001", 100000L, "COP", "CO", "MOCK_PSP",
                "https://webhook.com", "https://return.com", null, null, customer
        );

        Transaction updated = original.withStatus(TransactionStatus.APPROVED);

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getClientTransactionId()).isEqualTo(original.getClientTransactionId());
        assertThat(updated.getAmountCents()).isEqualTo(original.getAmountCents());
        assertThat(updated.getCurrencyCode()).isEqualTo(original.getCurrencyCode());
        assertThat(updated.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(updated.getCustomer()).isEqualTo(original.getCustomer());
        assertThat(updated.getProcessedAt()).isEqualTo(original.getProcessedAt());
        assertThat(updated.getCreatedAt()).isEqualTo(original.getCreatedAt());
    }
}
