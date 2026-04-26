package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository;

import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.CustomerAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para operaciones de persistencia de auditoría de clientes.
 */
@Repository
public interface CustomerAuditJpaRepository extends JpaRepository<CustomerAuditEntity, UUID> {

    List<CustomerAuditEntity> findByCustomerIdOrderByChangedAtDesc(UUID customerId);
}
