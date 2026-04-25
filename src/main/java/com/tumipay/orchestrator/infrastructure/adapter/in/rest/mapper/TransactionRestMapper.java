package com.tumipay.orchestrator.infrastructure.adapter.in.rest.mapper;

import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.port.in.CreateTransactionCommand;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CreateTransactionRequest;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir entre DTOs REST y objetos de dominio.
 * Transforma las peticiones HTTP en comandos de aplicación y las entidades de dominio en respuestas HTTP.
 */
@Mapper(componentModel = "spring")
public interface TransactionRestMapper {

    /**
     * Convierte una petición REST en un comando de creación de transacción.
     *
     * @param request la petición HTTP de creación de transacción
     * @return el comando de dominio para crear la transacción
     */
    @Mapping(target = "customer", expression = "java(toCustomer(request.getCustomer()))")
    CreateTransactionCommand toCommand(CreateTransactionRequest request);

    /**
     * Convierte un CustomerRequest en un objeto de dominio Customer.
     * Método por defecto para manejar la conversión anidada.
     *
     * @param req el DTO de petición del cliente
     * @return el objeto de valor Customer del dominio
     */
    default Customer toCustomer(com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CustomerRequest req) {
        if (req == null) return null;
        return Customer.builder()
                .documentType(req.getDocumentType())
                .documentNumber(req.getDocumentNumber())
                .countryCallingCode(req.getCountryCallingCode())
                .phoneNumber(req.getPhoneNumber())
                .email(req.getEmail())
                .firstName(req.getFirstName())
                .middleName(req.getMiddleName())
                .lastName(req.getLastName())
                .secondLastName(req.getSecondLastName())
                .build();
    }

    /**
     * Convierte una entidad de dominio Transaction en una respuesta REST.
     *
     * @param transaction la transacción del dominio
     * @return el DTO de respuesta para la API REST
     */
    @Mapping(target = "transactionId", source = "id")
    @Mapping(target = "status", expression = "java(transaction.getStatus().name())")
    TransactionResponse toResponse(Transaction transaction);
}
