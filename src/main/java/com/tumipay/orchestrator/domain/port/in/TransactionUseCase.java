package com.tumipay.orchestrator.domain.port.in;

import com.tumipay.orchestrator.domain.model.Transaction;

import java.util.UUID;

/**
 * Puerto primario (entrada) — define los casos de uso de la aplicación.
 * Sigue el patrón "Puertos y Adaptadores": el dominio expone lo que necesita;
 * la infraestructura se adapta a él.
 */
public interface TransactionUseCase {

    /**
     * Orquesta la creación y procesamiento de una nueva transacción.
     *
     * @param command todos los datos requeridos para crear una transacción
     * @return el objeto de dominio de transacción persistido
     */
    Transaction createTransaction(CreateTransactionCommand command);

    /**
     * Recupera una transacción por su identificador interno.
     *
     * @param transactionId el UUID asignado por este microservicio
     * @return el objeto de dominio de transacción
     */
    Transaction getTransaction(UUID transactionId);
}
