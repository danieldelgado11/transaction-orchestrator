package com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_shouldCreateSuccessResponseWithData() {
        // Given
        String data = "Test Data";

        // When
        ApiResponse<String> response = ApiResponse.success(data);

        // Then
        assertThat(response.getResponseCode()).isEqualTo("000");
        assertThat(response.getResponseMessage()).isEqualTo("Operación exitosa");
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    void success_withNullData_shouldCreateSuccessResponseWithNullData() {
        // When
        ApiResponse<Object> response = ApiResponse.success(null);

        // Then
        assertThat(response.getResponseCode()).isEqualTo("000");
        assertThat(response.getResponseMessage()).isEqualTo("Operación exitosa");
        assertThat(response.getData()).isNull();
    }

    @Test
    void success_withComplexObject_shouldCreateSuccessResponse() {
        // Given
        TransactionResponse transactionResponse = TransactionResponse.builder()
                .transactionId(java.util.UUID.randomUUID())
                .clientTransactionId("client-123")
                .status("PENDING")
                .build();

        // When
        ApiResponse<TransactionResponse> response = ApiResponse.success(transactionResponse);

        // Then
        assertThat(response.getResponseCode()).isEqualTo("000");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getClientTransactionId()).isEqualTo("client-123");
    }

    @Test
    void error_shouldCreateErrorResponseWithCodeAndMessage() {
        // Given
        String errorCode = "001";
        String errorMessage = "Error de validación";

        // When
        ApiResponse<Object> response = ApiResponse.error(errorCode, errorMessage);

        // Then
        assertThat(response.getResponseCode()).isEqualTo(errorCode);
        assertThat(response.getResponseMessage()).isEqualTo(errorMessage);
        assertThat(response.getData()).isNull();
    }

    @Test
    void error_withDifferentErrorCodes_shouldCreateProperResponses() {
        // Test different error codes
        ApiResponse<Object> error001 = ApiResponse.error("001", "Validación");
        ApiResponse<Object> error002 = ApiResponse.error("002", "Duplicado");
        ApiResponse<Object> error003 = ApiResponse.error("003", "No encontrado");
        ApiResponse<Object> error004 = ApiResponse.error("004", "Proveedor no disponible");
        ApiResponse<Object> error005 = ApiResponse.error("005", "Error interno");

        // Then
        assertThat(error001.getResponseCode()).isEqualTo("001");
        assertThat(error002.getResponseCode()).isEqualTo("002");
        assertThat(error003.getResponseCode()).isEqualTo("003");
        assertThat(error004.getResponseCode()).isEqualTo("004");
        assertThat(error005.getResponseCode()).isEqualTo("005");
    }

    @Test
    void builder_shouldCreateResponseWithAllFields() {
        // Given
        String data = "Custom Data";

        // When
        ApiResponse<String> response = ApiResponse.<String>builder()
                .responseCode("999")
                .responseMessage("Custom Message")
                .data(data)
                .build();

        // Then
        assertThat(response.getResponseCode()).isEqualTo("999");
        assertThat(response.getResponseMessage()).isEqualTo("Custom Message");
        assertThat(response.getData()).isEqualTo(data);
    }
}
