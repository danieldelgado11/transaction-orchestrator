package com.tumipay.orchestrator.domain.port.in;

import com.tumipay.orchestrator.domain.model.Customer;
import lombok.Builder;
import lombok.Getter;

/**
 * Objeto de comando que transporta todos los datos necesarios para crear una Transacción.
 * Los comandos son contenedores simples de datos — sin lógica de negocio aquí.
 */
@Getter
@Builder
public class CreateTransactionCommand {

    private final String clientTransactionId;
    private final Long amountCents;
    private final String currencyCode;
    private final String countryCode;
    private final String paymentMethodId;
    private final String webhookUrl;
    private final String redirectUrl;
    private final String description;
    private final Long expirationSeconds;
    private final Customer customer;
}
