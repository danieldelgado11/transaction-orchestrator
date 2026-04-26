package com.tumipay.orchestrator.infrastructure.adapter.out.provider;

import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para MockPaymentProviderAdapter.
 * Verifica el proveedor de pago simulado utilizado para pruebas,
 * incluyendo el método de procesamiento y el ID de método soportado.
 */
class MockPaymentProviderAdapterTest {

    private final MockPaymentProviderAdapter adapter = new MockPaymentProviderAdapter();

    @Test
    @DisplayName("Debe retornar MOCK_PSP como método de pago soportado")
    void getSupportedPaymentMethodId_shouldReturnMockPsp() {
        assertThat(adapter.getSupportedPaymentMethodId()).isEqualTo("MOCK_PSP");
    }

    @Test
    @DisplayName("Debe procesar transacción y retornar APPROVED")
    void process_shouldReturnApproved() {
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

        Transaction transaction = Transaction.create(
                "CLIENT-001", 100000L, "COP", "CO", "MOCK_PSP",
                "https://webhook.com", "https://return.com", "Test", 1800L, customer
        );

        TransactionStatus result = adapter.process(transaction);

        assertThat(result).isEqualTo(TransactionStatus.APPROVED);
    }
}
