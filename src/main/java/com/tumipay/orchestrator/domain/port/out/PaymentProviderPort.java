package com.tumipay.orchestrator.domain.port.out;

import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;

/**
 * Puerto secundario (salida) — contrato de adaptador de proveedor de pago.
 * Cada proveedor de pago (PSP) implementa esta interfaz como un adaptador.
 * Esto habilita el patrón Strategy: nuevos proveedores pueden agregarse sin
 * tocar las capas de dominio o aplicación.
 */
public interface PaymentProviderPort {

    /**
     * Retorna el identificador único del método de pago que soporta este adaptador.
     */
    String getSupportedPaymentMethodId();

    /**
     * Envía la transacción al proveedor de pago subyacente.
     *
     * @param transaction la transacción a procesar
     * @return el estado resultante después del procesamiento del proveedor
     */
    TransactionStatus process(Transaction transaction);
}
