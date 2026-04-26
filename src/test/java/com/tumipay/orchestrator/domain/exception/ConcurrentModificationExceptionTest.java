package com.tumipay.orchestrator.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ConcurrentModificationExceptionTest {

    @Test
    void constructor_shouldCreateExceptionWithCorrectMessage() {
        // Given
        String transactionId = "550e8400-e29b-41d4-a716-446655440000";

        // When
        ConcurrentModificationException exception = new ConcurrentModificationException(transactionId);

        // Then
        assertThat(exception.getMessage())
                .contains("La transacción " + transactionId + " fue modificada por otro proceso");
    }

    @Test
    void exception_shouldBeRuntimeException() {
        // Given & When & Then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> {
                    throw new ConcurrentModificationException("test-id");
                })
                .withMessageContaining("test-id");
    }

    @Test
    void exception_shouldInheritFromRuntimeException() {
        // Given
        ConcurrentModificationException exception = new ConcurrentModificationException("test-id");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
