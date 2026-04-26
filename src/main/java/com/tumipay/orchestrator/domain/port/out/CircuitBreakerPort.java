package com.tumipay.orchestrator.domain.port.out;

import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;

/**
 * Puerto secundario (salida) — abstracción del Circuit Breaker.
 * Define el contrato para ejecutar operaciones con protección de tolerancia a fallos.
 *
 * Implementado en la capa de infraestructura usando Resilience4j u otra librería.
 * Esto permite cambiar la implementación del Circuit Breaker sin afectar el dominio
 * o la capa de aplicación.
 */
public interface CircuitBreakerPort {

    /**
     * Ejecuta el procesamiento de una transacción a través de un proveedor de pago
     * con protección de Circuit Breaker.
     *
     * @param provider el proveedor de pago a utilizar
     * @param transaction la transacción a procesar
     * @return el estado resultante del procesamiento
     */
    TransactionStatus executeWithCircuitBreaker(PaymentProviderPort provider, Transaction transaction);
}
