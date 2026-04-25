package com.tumipay.orchestrator.infrastructure.adapter.out.persistence;

import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.port.out.TransactionRepository;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.JpaTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida — implementa el puerto de persistencia del dominio usando JPA.
 * El dominio nunca ve Spring Data o JPA; solo conoce la interfaz TransactionRepository.
 */
@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepository {

    private final JpaTransactionRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = mapper.toEntity(transaction);
        TransactionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
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
