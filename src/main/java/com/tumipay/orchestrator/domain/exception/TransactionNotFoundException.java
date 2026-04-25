package com.tumipay.orchestrator.domain.exception;

/**
 * Lanzada cuando no se puede encontrar una transacción por su identificador.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String id) {
        super("Transacción no encontrada con id: " + id);
    }
}
