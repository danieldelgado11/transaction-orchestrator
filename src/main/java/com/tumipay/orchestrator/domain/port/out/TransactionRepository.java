package com.tumipay.orchestrator.domain.port.out;

import com.tumipay.orchestrator.domain.model.Transaction;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario (salida) — contrato de persistencia.
 * El dominio define QUÉ necesita; la infraestructura provee CÓMO.
 */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID id);

    boolean existsByClientTransactionId(String clientTransactionId);
}
