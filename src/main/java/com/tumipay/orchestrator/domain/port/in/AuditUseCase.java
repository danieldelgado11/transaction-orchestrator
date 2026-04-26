package com.tumipay.orchestrator.domain.port.in;

import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;

/**
 * Puerto de entrada (primario) — casos de uso de auditoría.
 * Define las operaciones de auditoría que el dominio expone.
 * Implementado en la capa de aplicación.
 */
public interface AuditUseCase {

    /**
     * Registra auditoría para la creación de una transacción.
     * Incluye auditoría del cliente asociado solo si es un cliente nuevo.
     *
     * @param transaction la transacción creada
     * @param isNewCustomer true si el cliente fue creado nuevo, false si ya existía
     */
    void auditTransactionCreation(Transaction transaction, boolean isNewCustomer);

    /**
     * Registra auditoría para la actualización de estado de una transacción.
     *
     * @param transaction la transacción actualizada
     * @param oldStatus   el estado anterior
     */
    void auditTransactionUpdate(Transaction transaction, TransactionStatus oldStatus);
}
