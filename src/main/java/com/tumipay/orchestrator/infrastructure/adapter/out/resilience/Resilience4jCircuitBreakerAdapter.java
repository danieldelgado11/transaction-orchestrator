package com.tumipay.orchestrator.infrastructure.adapter.out.resilience;

import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;
import com.tumipay.orchestrator.domain.port.out.CircuitBreakerPort;
import com.tumipay.orchestrator.domain.port.out.PaymentProviderPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que implementa Circuit Breaker usando Resilience4j.
 * Este adaptador pertenece a la capa de infraestructura y encapsula
 * el framework Resilience4j, manteniendo el dominio y aplicación libres
 * de dependencias externas.
 *
 * Implementa el puerto CircuitBreakerPort definido en el dominio.
 */
@Slf4j
@Component
public class Resilience4jCircuitBreakerAdapter implements CircuitBreakerPort {

    private static final String CIRCUIT_BREAKER_NAME = "payment-provider";

    /**
     * Ejecuta el procesamiento con protección de Circuit Breaker.
     * Si el circuito está abierto, invoca el fallback method automáticamente.
     *
     * @param provider el proveedor de pago a utilizar
     * @param transaction la transacción a procesar
     * @return el estado resultante del procesamiento
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackProcess")
    public TransactionStatus executeWithCircuitBreaker(PaymentProviderPort provider, Transaction transaction) {
        log.debug("Ejecutando con Circuit Breaker: provider={}, transactionId={}",
                provider.getSupportedPaymentMethodId(), transaction.getId());
        return provider.process(transaction);
    }

    /**
     * Fallback method cuando el Circuit Breaker está abierto u ocurre un error.
     * Marca la transacción como FAILED para permitir reintento posterior.
     *
     * @param provider el proveedor que falló
     * @param transaction la transacción que se intentó procesar
     * @param throwable la excepción que causó el fallo
     * @return FAILED para permitir reintentos manuales o automáticos
     */
    @SuppressWarnings("unused")
    private TransactionStatus fallbackProcess(PaymentProviderPort provider, Transaction transaction, Throwable throwable) {
        log.warn("Circuit Breaker activado para proveedor {}: {}. Marcando transacción {} como FAILED",
                provider.getSupportedPaymentMethodId(),
                throwable.getMessage(),
                transaction.getId());
        return TransactionStatus.FAILED;
    }
}
