package com.tumipay.orchestrator.domain.exception;

/**
 * Lanzada cuando se detecta un clientTransactionId duplicado.
 */
public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String clientTransactionId) {
        super("Ya existe una transacción con clientTransactionId: " + clientTransactionId);
    }
}
