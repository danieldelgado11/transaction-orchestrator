package com.tumipay.orchestrator.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerAuditEventTest {

    @Test
    void fromCustomer_shouldCreateEventWithAllFields() {
        // Given
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("123456789")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("John")
                .middleName("Doe")
                .lastName("Smith")
                .secondLastName("Johnson")
                .build();

        UUID customerId = UUID.randomUUID();
        String changedBy = "TestService";
        UUID transactionId = UUID.randomUUID();

        // When
        CustomerAuditEvent event = CustomerAuditEvent.fromCustomer(
                customer, AuditAction.INSERT, customerId, changedBy, transactionId);

        // Then
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getAction()).isEqualTo(AuditAction.INSERT);
        assertThat(event.getDocumentType()).isEqualTo("CC");
        assertThat(event.getDocumentNumber()).isEqualTo("123456789");
        assertThat(event.getCountryCallingCode()).isEqualTo("+57");
        assertThat(event.getPhoneNumber()).isEqualTo("3001234567");
        assertThat(event.getEmail()).isEqualTo("test@example.com");
        assertThat(event.getFirstName()).isEqualTo("John");
        assertThat(event.getMiddleName()).isEqualTo("Doe");
        assertThat(event.getLastName()).isEqualTo("Smith");
        assertThat(event.getSecondLastName()).isEqualTo("Johnson");
        assertThat(event.getChangedBy()).isEqualTo(changedBy);
        assertThat(event.getTransactionId()).isEqualTo(transactionId);
        assertThat(event.getChangedAt()).isNotNull();
    }

    @Test
    void fromCustomer_withUpdateAction_shouldCreateEventWithUpdateAction() {
        // Given
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .documentType("NIT")
                .documentNumber("987654321")
                .countryCallingCode("+1")
                .phoneNumber("5551234")
                .email("company@example.com")
                .firstName("Company")
                .lastName("Name")
                .build();

        // When
        CustomerAuditEvent event = CustomerAuditEvent.fromCustomer(
                customer, AuditAction.UPDATE, customer.getId(), "AuditService", UUID.randomUUID());

        // Then
        assertThat(event.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(event.getDocumentType()).isEqualTo("NIT");
    }

    @Test
    void builder_shouldCreateEventWithAllFields() {
        // Given
        UUID customerId = UUID.randomUUID();

        // When
        CustomerAuditEvent event = CustomerAuditEvent.builder()
                .customerId(customerId)
                .action(AuditAction.DELETE)
                .documentType("CC")
                .documentNumber("123")
                .changedBy("Test")
                .build();

        // Then
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getAction()).isEqualTo(AuditAction.DELETE);
    }
}
