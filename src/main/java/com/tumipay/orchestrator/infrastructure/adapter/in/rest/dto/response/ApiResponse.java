package com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Sobre genérico de respuesta de API.
 * Usa un campo de datos genérico para que cualquier tipo de carga útil pueda envolverse.
 *
 * Diccionario de errores:
 *   000 - Operación exitosa
 *   001 - Error de validación (campos inválidos o faltantes)
 *   002 - clientTransactionId duplicado
 *   003 - Transacción no encontrada
 *   004 - Proveedor de pago no disponible para el método solicitado
 *   005 - Error interno del servidor
 */
@Getter
@Builder
public class ApiResponse<T> {

    @JsonProperty("response_code")
    private final String responseCode;

    @JsonProperty("response_message")
    private final String responseMessage;

    @JsonProperty("data")
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .responseCode("000")
                .responseMessage("Operación exitosa")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .responseCode(code)
                .responseMessage(message)
                .data(null)
                .build();
    }
}
