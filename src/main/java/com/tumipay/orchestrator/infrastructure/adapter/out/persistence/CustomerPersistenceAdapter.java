package com.tumipay.orchestrator.infrastructure.adapter.out.persistence;

import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.port.out.CustomerRepository;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.JpaCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida — implementa el puerto de persistencia de clientes usando JPA.
 *
 * Decisiones de diseño:
 * - Idempotencia SOLO por documento (tipo + número).
 *   El email se puede actualizar pero no se usa para buscar cliente existente.
 * - Si el cliente existe por documento, actualiza los datos (nombres, teléfono, email, etc.)
 *   pero mantiene el ID. Esto permite que un cliente tenga múltiples transacciones.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerPersistenceAdapter implements CustomerRepository {

    private final JpaCustomerRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = mapper.toCustomerEntity(customer);
        CustomerEntity saved = jpaRepository.save(entity);
        return mapper.toCustomer(saved);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toCustomer);
    }

    @Override
    public Optional<Customer> findByDocument(String documentType, String documentNumber) {
        return jpaRepository.findByDocumentTypeAndDocumentNumber(documentType, documentNumber)
                .map(mapper::toCustomer);
    }

    @Override
    public CustomerResult findOrCreate(Customer customer) {
        // Idempotencia SOLO por documento (tipo + número)
        Optional<Customer> existingByDocument = findByDocument(
                customer.getDocumentType(),
                customer.getDocumentNumber()
        );

        if (existingByDocument.isPresent()) {
            log.debug("Cliente existente encontrado por documento {}/{}",
                    customer.getDocumentType(), customer.getDocumentNumber());
            Customer updated = updateExistingCustomer(existingByDocument.get(), customer);
            return new CustomerResult(updated, false); // Cliente existente, no es nuevo
        }

        // No existe, crear nuevo
        log.debug("Creando nuevo cliente: {}/{}",
                customer.getDocumentType(), customer.getDocumentNumber());
        Customer newCustomer = save(customer);
        return new CustomerResult(newCustomer, true); // Cliente nuevo
    }

    /**
     * Actualiza los datos de un cliente existente manteniendo su ID.
     * Permite que los datos evolucionen (cambio de teléfono, email, etc.)
     * sin perder el historial de transacciones.
     */
    private Customer updateExistingCustomer(Customer existing, Customer newData) {
        // Crear un nuevo Customer con el ID existente pero datos actualizados
        Customer updated = Customer.builder()
                .id(existing.getId())
                .documentType(existing.getDocumentType()) // El documento no cambia
                .documentNumber(existing.getDocumentNumber())
                .countryCallingCode(newData.getCountryCallingCode())
                .phoneNumber(newData.getPhoneNumber())
                .email(newData.getEmail())
                .firstName(newData.getFirstName())
                .middleName(newData.getMiddleName())
                .lastName(newData.getLastName())
                .secondLastName(newData.getSecondLastName())
                .build();

        return save(updated);
    }
}
