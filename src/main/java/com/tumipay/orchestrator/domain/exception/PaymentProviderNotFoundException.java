package com.tumipay.orchestrator.domain.exception;

/**
 * Lanzada cuando no hay un adaptador registrado para el método de pago solicitado.
 */
public class PaymentProviderNotFoundException extends RuntimeException {

    public PaymentProviderNotFoundException(String paymentMethodId) {
        super("No hay un adaptador de proveedor de pago registrado para paymentMethodId: " + paymentMethodId);
    }
}
