package com.tumipay.orchestrator.infrastructure.adapter.in.rest.controller;

import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.port.in.CreateTransactionCommand;
import com.tumipay.orchestrator.domain.port.in.TransactionUseCase;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CreateTransactionRequest;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.TransactionResponse;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.mapper.TransactionRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Adaptador HTTP de entrada — traduce peticiones HTTP en comandos de aplicación.
 * NO contiene ninguna lógica de negocio; solo delega al puerto de caso de uso.
 *
 * Versionado de API mediante ruta URI (/v1/) siguiendo mejores prácticas REST.
 */
@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transacciones", description = "API de Orquestación de Transacciones")
public class TransactionController {

    private final TransactionUseCase transactionUseCase;
    private final TransactionRestMapper mapper;

    /**
     * POST /v1/transactions
     * Crea y orquesta una nueva transacción de pago.
     *
     * @param request cuerpo de la petición validado
     * @return 201 CREATED con datos de la transacción en caso de éxito
     */
    @PostMapping
    @Operation(summary = "Crear una nueva transacción", description = "Orquesta una transacción de pago a través del adaptador de proveedor apropiado")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {

        CreateTransactionCommand command = mapper.toCommand(request);
        Transaction transaction = transactionUseCase.createTransaction(command);
        TransactionResponse response = mapper.toResponse(transaction);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * GET /v1/transactions/{transaction_id}
     * Recupera una transacción por su identificador interno.
     *
     * @param transactionId el UUID asignado por este microservicio
     * @return 200 OK con datos de la transacción
     */
    @GetMapping("/{transaction_id}")
    @Operation(summary = "Obtener transacción por ID", description = "Recupera los detalles completos de la transacción mediante el UUID interno de transacción")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable("transaction_id") UUID transactionId) {

        Transaction transaction = transactionUseCase.getTransaction(transactionId);
        TransactionResponse response = mapper.toResponse(transaction);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
