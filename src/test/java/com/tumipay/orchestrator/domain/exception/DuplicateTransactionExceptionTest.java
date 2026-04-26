package com.tumipay.orchestrator.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DuplicateTransactionExceptionTest {

    @Test
    void constructor_shouldCreateExceptionWithCorrectMessage() {
        // Given
        String clientTransactionId = "client-tx-123";

        // When
        DuplicateTransactionException exception = new DuplicateTransactionException(clientTransactionId);

        // Then
        assertThat(exception.getMessage()).contains("Ya existe una transacción con clientTransactionId: " + clientTransactionId);
    }

    @Test
    void exception_shouldBeRuntimeException() {
        // Given & When & Then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> {
                    throw new DuplicateTransactionException("test-id");
                })
                .withMessageContaining("test-id");
    }

    @Test
    void exception_shouldInheritFromRuntimeException() {
        // Given
        DuplicateTransactionException exception = new DuplicateTransactionException("test-id");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
