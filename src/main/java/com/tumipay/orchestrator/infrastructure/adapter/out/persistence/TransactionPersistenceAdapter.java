package com.tumipay.orchestrator.infrastructure.adapter.out.persistence;

import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.port.out.TransactionRepository;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.JpaCustomerRepository;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.JpaTransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida — implementa el puerto de persistencia del dominio usando JPA.
 * El dominio nunca ve Spring Data o JPA; solo conoce la interfaz TransactionRepository.
 *
 * Decisiones de diseño:
 * - Para evitar 'DuplicateKeyException: A different object with the same identifier was already associated',
 *   buscamos primero la entidad existente en la sesión antes de crear una nueva.
 * - Si la entidad existe, actualizamos sus campos en lugar de crear una nueva instancia.
 * - Esto mantiene la identidad de objeto única en la sesión de Hibernate y permite
 *   que el optimistic locking (@Version) funcione correctamente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepository {

    private final JpaTransactionRepository jpaRepository;
    private final JpaCustomerRepository jpaCustomerRepository;
    private final TransactionPersistenceMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Transaction save(Transaction transaction) {

        Optional<TransactionEntity> existingEntity = jpaRepository.findById(transaction.getId());

        TransactionEntity entityToSave;
        if (existingEntity.isPresent()) {
            entityToSave = existingEntity.get();
            updateEntityFields(entityToSave, transaction);
        } else {
            entityToSave = mapper.toEntity(transaction);

            CustomerEntity attachedCustomer = jpaCustomerRepository
                    .findById(transaction.getCustomer().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Customer no encontrado: " + transaction.getCustomer().getId()));
            entityToSave.setCustomer(attachedCustomer);
        }

        try {
            TransactionEntity saved = jpaRepository.save(entityToSave);
            // Forzar flush para detectar conflictos de versión inmediatamente
            entityManager.flush();
            return mapper.toDomain(saved);
        } catch (OptimisticLockException e) {
            log.warn("Conflicto de optimistic locking al guardar transacción {}", transaction.getId());
            throw new com.tumipay.orchestrator.domain.exception.ConcurrentModificationException(
                    transaction.getId().toString());
        }
    }

    /**
     * Actualiza los campos de la entidad existente con los valores del modelo de dominio.
     * Esto evita crear una nueva instancia de entidad que causaría conflictos en la sesión de Hibernate.
     */
    private void updateEntityFields(TransactionEntity entity, Transaction transaction) {
        entity.setStatus(transaction.getStatus().name());
        entity.setProcessedAt(transaction.getProcessedAt());
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByClientTransactionId(String clientTransactionId) {
        return jpaRepository.existsByClientTransactionId(clientTransactionId);
    }
}
