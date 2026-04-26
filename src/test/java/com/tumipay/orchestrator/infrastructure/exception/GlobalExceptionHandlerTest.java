package com.tumipay.orchestrator.infrastructure.exception;

import com.tumipay.orchestrator.domain.exception.ConcurrentModificationException;
import com.tumipay.orchestrator.domain.exception.DuplicateTransactionException;
import com.tumipay.orchestrator.domain.exception.PaymentProviderNotFoundException;
import com.tumipay.orchestrator.domain.exception.TransactionNotFoundException;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GlobalExceptionHandler - verifies proper error code mapping.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Debe retornar 409 CONFLICT con código 006 para modificación concurrente")
    void handleConcurrentModification_returnsConflict() {
        // Given
        ConcurrentModificationException ex = new ConcurrentModificationException("txn-123");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleConcurrentModification(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponseCode()).isEqualTo("006");
        assertThat(response.getBody().getResponseMessage()).contains("txn-123");
    }

    @Test
    @DisplayName("Debe retornar 409 CONFLICT con código 002 para transacción duplicada")
    void handleDuplicate_returnsConflict() {
        // Given
        DuplicateTransactionException ex = new DuplicateTransactionException("client-001");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleDuplicate(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponseCode()).isEqualTo("002");
    }

    @Test
    @DisplayName("Debe retornar 404 NOT_FOUND con código 003 cuando transacción no se encuentra")
    void handleNotFound_returnsNotFound() {
        // Given
        TransactionNotFoundException ex = new TransactionNotFoundException("txn-456");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponseCode()).isEqualTo("003");
    }

    @Test
    @DisplayName("Debe retornar 400 BAD_REQUEST con código 004 cuando proveedor no se encuentra")
    void handleProviderNotFound_returnsBadRequest() {
        // Given
        PaymentProviderNotFoundException ex = new PaymentProviderNotFoundException("UNKNOWN_PSP");

        // When
        ResponseEntity<ApiResponse<Void>> response = handler.handleProviderNotFound(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponseCode()).isEqualTo("004");
    }
}
