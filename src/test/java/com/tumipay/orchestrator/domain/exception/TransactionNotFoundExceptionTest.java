package com.tumipay.orchestrator.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TransactionNotFoundExceptionTest {

    @Test
    void constructor_shouldCreateExceptionWithCorrectMessage() {
        // Given
        String transactionId = "550e8400-e29b-41d4-a716-446655440000";

        // When
        TransactionNotFoundException exception = new TransactionNotFoundException(transactionId);

        // Then
        assertThat(exception.getMessage()).contains("Transacción no encontrada con id: " + transactionId);
    }

    @Test
    void exception_shouldBeRuntimeException() {
        // Given & When & Then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> {
                    throw new TransactionNotFoundException("test-id");
                })
                .withMessageContaining("test-id");
    }

    @Test
    void exception_shouldInheritFromRuntimeException() {
        // Given
        TransactionNotFoundException exception = new TransactionNotFoundException("test-id");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
