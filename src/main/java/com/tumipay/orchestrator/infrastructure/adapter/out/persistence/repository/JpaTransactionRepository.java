package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository;

import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA para operaciones de persistencia de transacciones.
 * Proporciona métodos de consulta derivados del nombre del método.
 */
public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    /**
     * Verifica si existe una transacción con el clientTransactionId especificado.
     *
     * @param clientTransactionId el identificador de transacción del cliente a verificar
     * @return true si existe una transacción con ese identificador, false en caso contrario
     */
    boolean existsByClientTransactionId(String clientTransactionId);
}
