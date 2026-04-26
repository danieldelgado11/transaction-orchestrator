package com.tumipay.orchestrator.domain.port.out;

import com.tumipay.orchestrator.domain.model.Customer;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de clientes.
 * Implementa el patrón Repository para la entidad Customer.
 */
public interface CustomerRepository {

    /**
     * Guarda un cliente (crea o actualiza si ya existe).
     *
     * @param customer el cliente a guardar
     * @return el cliente guardado con ID asignado
     */
    Customer save(Customer customer);

    /**
     * Busca un cliente por su ID.
     *
     * @param id el ID del cliente
     * @return Optional con el cliente si existe
     */
    Optional<Customer> findById(java.util.UUID id);

    /**
     * Busca un cliente por tipo y número de documento.
     * Usado para idempotencia: evitar crear duplicados del mismo cliente.
     *
     * @param documentType tipo de documento (CC, NIT, etc.)
     * @param documentNumber número del documento
     * @return Optional con el cliente si existe
     */
    Optional<Customer> findByDocument(String documentType, String documentNumber);

    /**
     * Busca o crea un cliente.
     * Idempotencia SOLO por documento (tipo + número).
     * Si existe, actualiza los datos. Si no existe, lo crea.
     *
     * @param customer datos del cliente
     * @return resultado con el cliente y flag indicando si fue creado nuevo
     */
    CustomerResult findOrCreate(Customer customer);

    /**
     * Resultado de la operación findOrCreate.
     * Contiene el cliente y un flag indicando si fue creado nuevo.
     */
    record CustomerResult(Customer customer, boolean isNew) {}
}
