package com.tumipay.orchestrator.domain.port.out;

import com.tumipay.orchestrator.domain.model.CustomerAuditEvent;
import com.tumipay.orchestrator.domain.model.TransactionAuditEvent;

/**
 * Puerto secundario (salida) — contrato de persistencia para auditoría.
 * Define cómo se almacenan los registros de auditoría en la base de datos.
 */
public interface AuditRepositoryPort {

    /**
     * Guarda un registro de auditoría de cliente.
     *
     * @param event el evento de auditoría a guardar
     */
    void saveCustomerAudit(CustomerAuditEvent event);

    /**
     * Guarda un registro de auditoría de transacción.
     *
     * @param event el evento de auditoría a guardar
     */
    void saveTransactionAudit(TransactionAuditEvent event);
}
