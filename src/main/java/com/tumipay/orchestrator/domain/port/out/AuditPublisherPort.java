package com.tumipay.orchestrator.domain.port.out;

import com.tumipay.orchestrator.domain.model.CustomerAuditEvent;
import com.tumipay.orchestrator.domain.model.TransactionAuditEvent;

/**
 * Puerto secundario (salida) — contrato para publicar eventos de auditoría.
 * Permite desacoplar el dominio de la infraestructura de mensajería (Kafka).
 */
public interface AuditPublisherPort {

    /**
     * Publica un evento de auditoría de cliente.
     *
     * @param event el evento de auditoría a publicar
     */
    void publishCustomerAudit(CustomerAuditEvent event);

    /**
     * Publica un evento de auditoría de transacción.
     *
     * @param event el evento de auditoría a publicar
     */
    void publishTransactionAudit(TransactionAuditEvent event);
}
