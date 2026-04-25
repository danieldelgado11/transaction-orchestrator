package com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para los datos de una transacción.
 * Representa la información devuelta al cliente después de crear o consultar una transacción.
 */
@Getter
@Builder
public class TransactionResponse {

    @JsonProperty("transaction_id")
    private final UUID transactionId;

    @JsonProperty("processed_at")
    private final LocalDateTime processedAt;

    @JsonProperty("client_transaction_id")
    private final String clientTransactionId;

    @JsonProperty("payment_method_id")
    private final String paymentMethodId;

    @JsonProperty("currency_code")
    private final String currencyCode;

    @JsonProperty("country_code")
    private final String countryCode;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("status")
    private final String status;
}
