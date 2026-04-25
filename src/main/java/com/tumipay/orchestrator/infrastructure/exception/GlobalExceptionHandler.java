package com.tumipay.orchestrator.infrastructure.exception;

import com.tumipay.orchestrator.domain.exception.DuplicateTransactionException;
import com.tumipay.orchestrator.domain.exception.PaymentProviderNotFoundException;
import com.tumipay.orchestrator.domain.exception.TransactionNotFoundException;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Manejo centralizado de errores — traduce excepciones de dominio y validación
 * en respuestas de error de API estandarizadas.
 *
 * Diccionario de Códigos de Error:
 *   000 - Operación exitosa
 *   001 - Error de validación
 *   002 - Transacción duplicada
 *   003 - Transacción no encontrada
 *   004 - Proveedor de pago no encontrado
 *   005 - Error interno del servidor
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("001", message));
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateTransactionException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("002", ex.getMessage()));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(TransactionNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("003", ex.getMessage()));
    }

    @ExceptionHandler(PaymentProviderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProviderNotFound(PaymentProviderNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("004", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("001", "Formato de parámetro inválido: " + ex.getName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Excepción no manejada", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("005", "Error interno del servidor"));
    }
}
