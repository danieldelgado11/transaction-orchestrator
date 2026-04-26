package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository;

import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.TransactionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para operaciones de persistencia de auditoría de transacciones.
 */
@Repository
public interface TransactionAuditJpaRepository extends JpaRepository<TransactionAuditEntity, UUID> {

    List<TransactionAuditEntity> findByTransactionIdOrderByChangedAtDesc(UUID transactionId);
}
