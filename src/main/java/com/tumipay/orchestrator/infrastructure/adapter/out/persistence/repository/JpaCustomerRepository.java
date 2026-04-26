package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository;

import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA para la entidad Customer.
 * Proporciona operaciones CRUD y búsquedas específicas para idempotencia.
 */
@Repository
public interface JpaCustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    /**
     * Busca un cliente por tipo y número de documento.
     * Usado para idempotencia: evitar crear duplicados.
     *
     * @param documentType tipo de documento (CC, NIT, etc.)
     * @param documentNumber número del documento
     * @return Optional con la entidad si existe
     */
    Optional<CustomerEntity> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    /**
     * Busca un cliente por email.
     * Usado para idempotencia: evitar crear duplicados.
     *
     * @param email el email del cliente
     * @return Optional con la entidad si existe
     */
    Optional<CustomerEntity> findByEmail(String email);

    /**
     * Verifica si existe un cliente con el documento dado.
     *
     * @param documentType tipo de documento
     * @param documentNumber número del documento
     * @return true si existe
     */
    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    /**
     * Verifica si existe un cliente con el email dado.
     *
     * @param email el email
     * @return true si existe
     */
    boolean existsByEmail(String email);
}
