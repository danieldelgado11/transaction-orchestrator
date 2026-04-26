package com.tumipay.orchestrator.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PaymentProviderNotFoundExceptionTest {

    @Test
    void constructor_shouldCreateExceptionWithCorrectMessage() {
        // Given
        String paymentMethodId = "UNKNOWN_METHOD";

        // When
        PaymentProviderNotFoundException exception = new PaymentProviderNotFoundException(paymentMethodId);

        // Then
        assertThat(exception.getMessage())
                .contains("No hay un adaptador de proveedor de pago registrado para paymentMethodId: " + paymentMethodId);
    }

    @Test
    void exception_shouldBeRuntimeException() {
        // Given & When & Then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> {
                    throw new PaymentProviderNotFoundException("INVALID");
                })
                .withMessageContaining("INVALID");
    }

    @Test
    void exception_shouldInheritFromRuntimeException() {
        // Given
        PaymentProviderNotFoundException exception = new PaymentProviderNotFoundException("TEST");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
