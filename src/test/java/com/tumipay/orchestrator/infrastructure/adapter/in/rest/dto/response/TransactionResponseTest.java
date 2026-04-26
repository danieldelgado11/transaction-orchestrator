package com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionResponseTest {

    @Test
    void builder_shouldCreateResponseWithAllFields() {
        // Given
        UUID transactionId = UUID.randomUUID();
        LocalDateTime processedAt = LocalDateTime.now();

        // When
        TransactionResponse response = TransactionResponse.builder()
                .transactionId(transactionId)
                .processedAt(processedAt)
                .clientTransactionId("client-123")
                .paymentMethodId("NEQUI")
                .currencyCode("COP")
                .countryCode("CO")
                .description("Test transaction")
                .status("PENDING")
                .build();

        // Then
        assertThat(response.getTransactionId()).isEqualTo(transactionId);
        assertThat(response.getProcessedAt()).isEqualTo(processedAt);
        assertThat(response.getClientTransactionId()).isEqualTo("client-123");
        assertThat(response.getPaymentMethodId()).isEqualTo("NEQUI");
        assertThat(response.getCurrencyCode()).isEqualTo("COP");
        assertThat(response.getCountryCode()).isEqualTo("CO");
        assertThat(response.getDescription()).isEqualTo("Test transaction");
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void builder_withMinimalFields_shouldCreateResponse() {
        // When
        TransactionResponse response = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .clientTransactionId("client-min")
                .status("APPROVED")
                .build();

        // Then
        assertThat(response.getTransactionId()).isNotNull();
        assertThat(response.getClientTransactionId()).isEqualTo("client-min");
        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getProcessedAt()).isNull();
        assertThat(response.getDescription()).isNull();
    }

    @Test
    void builder_withDifferentStatuses_shouldCreateProperResponses() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        TransactionResponse pending = TransactionResponse.builder()
                .transactionId(id)
                .clientTransactionId("tx-1")
                .status("PENDING")
                .build();

        TransactionResponse approved = TransactionResponse.builder()
                .transactionId(id)
                .clientTransactionId("tx-2")
                .status("APPROVED")
                .build();

        TransactionResponse rejected = TransactionResponse.builder()
                .transactionId(id)
                .clientTransactionId("tx-3")
                .status("REJECTED")
                .build();

        // Then
        assertThat(pending.getStatus()).isEqualTo("PENDING");
        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
    }
}
