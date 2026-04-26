package com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para la creación de transacciones.
 * Todos los nombres de campos usan snake_case según especificación del contrato API.
 * Las anotaciones de validación refuerzan las reglas de campos obligatorios en la capa adaptadora HTTP.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateTransactionRequest {

    @NotBlank(message = "client_transaction_id es obligatorio")
    @Size(max = 100, message = "client_transaction_id no debe exceder 100 caracteres")
    @JsonProperty("client_transaction_id")
    private String clientTransactionId;

    @NotNull(message = "amount_cents es obligatorio")
    @Positive(message = "amount_cents debe ser un entero positivo")
    @JsonProperty("amount_cents")
    private Long amountCents;

    @NotBlank(message = "currency_code es obligatorio")
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency_code debe ser un código ISO 4217 de 3 letras")
    @JsonProperty("currency_code")
    private String currencyCode;

    @NotBlank(message = "country_code es obligatorio")
    @Size(min = 2, max = 2, message = "country_code debe ser exactamente 2 caracteres")
    @Pattern(regexp = "^[A-Z]{2}$", message = "country_code debe ser un código ISO 3166-1 Alpha-2 de 2 letras")
    @JsonProperty("country_code")
    private String countryCode;

    @NotBlank(message = "payment_method_id es obligatorio")
    @JsonProperty("payment_method_id")
    private String paymentMethodId;

    @NotBlank(message = "webhook_url es obligatorio")
    @Pattern(regexp = "^https?://.*", message = "webhook_url debe ser una URL válida")
    @JsonProperty("webhook_url")
    private String webhookUrl;

    @NotBlank(message = "redirect_url es obligatorio")
    @Pattern(regexp = "^https?://.*", message = "redirect_url debe ser una URL válida")
    @JsonProperty("redirect_url")
    private String redirectUrl;

    @Size(max = 255, message = "description no debe exceder 255 caracteres")
    @JsonProperty("description")
    private String description;

    @Positive(message = "expiration_seconds debe ser positivo")
    @JsonProperty("expiration_seconds")
    private Long expirationSeconds;

    @NotNull(message = "customer es obligatorio")
    @Valid
    @JsonProperty("customer")
    private CustomerRequest customer;
}
