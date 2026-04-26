package com.tumipay.orchestrator.domain.exception;

/**
 * Lanzada cuando se detecta una modificación concurrente de una transacción.
 * Indica que otro proceso modificó la entidad mientras se intentaba actualizar.
 */
public class ConcurrentModificationException extends RuntimeException {

    public ConcurrentModificationException(String transactionId) {
        super("La transacción " + transactionId + " fue modificada por otro proceso. Por favor, reintente la operación.");
    }
}
