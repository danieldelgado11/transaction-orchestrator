package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.mapper;

import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir entre entidades JPA y objetos de dominio.
 * Aísla la capa de persistencia del modelo de dominio.
 */
@Mapper(componentModel = "spring")
public interface TransactionPersistenceMapper {

    /**
     * Convierte un objeto de dominio Transaction en una entidad JPA.
     *
     * @param transaction el objeto de dominio a convertir
     * @return la entidad JPA lista para persistir
     */
    @Mapping(target = "status", expression = "java(transaction.getStatus().name())")
    @Mapping(target = "customer", expression = "java(toCustomerEntity(transaction.getCustomer()))")
    @Mapping(target = "updatedAt", ignore = true)
    TransactionEntity toEntity(Transaction transaction);

    /**
     * Convierte una entidad JPA en un objeto de dominio Transaction.
     *
     * @param entity la entidad JPA recuperada de la base de datos
     * @return el objeto de dominio correspondiente
     */
    @Mapping(target = "status", expression = "java(com.tumipay.orchestrator.domain.model.TransactionStatus.valueOf(entity.getStatus()))")
    @Mapping(target = "customer", expression = "java(toCustomer(entity.getCustomer()))")
    Transaction toDomain(TransactionEntity entity);

    /**
     * Convierte un objeto de dominio Customer en una entidad CustomerEntity.
     * Customer es una Entity con identidad propia.
     *
     * @param customer el objeto de dominio
     * @return la entidad JPA correspondiente
     */
    @Mapping(target = "id", source = "id")
    CustomerEntity toCustomerEntity(Customer customer);

    /**
     * Convierte una entidad CustomerEntity en un objeto de dominio Customer.
     * Customer es una Entity con identidad propia.
     *
     * @param entity la entidad JPA
     * @return el objeto de dominio
     */
    @Mapping(target = "id", source = "id")
    Customer toCustomer(CustomerEntity entity);
}
