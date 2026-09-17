package com.semillatecnologica.backend.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper estándar para todas las respuestas HTTP del sistema.
 *
 * <p>Cada respuesta del backend se envuelve en esta estructura para
 * garantizar un contrato uniforme entre el servidor y los consumidores.</p>
 *
 * @param <T> Tipo del dato contenido en la respuesta
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Indica si la operación fue exitosa.
     */
    private boolean success;

    /**
     * Mensaje descriptivo del resultado de la operación.
     */
    private String message;

    /**
     * Datos de la respuesta. Null cuando la operación no devuelve datos.
     */
    private T data;

    /**
     * Código de dominio del error. Null cuando la operación es exitosa.
     */
    private String code;

    /**
     * Errores de validación por campo. Null cuando no aplica.
     */
    private java.util.Map<String, java.util.List<String>> fields;

    /**
     * Identificador de correlación para rastrear la solicitud en logs.
     */
    private String requestId;

    /**
     * Construye una respuesta exitosa con datos.
     *
     * @param message Mensaje descriptivo
     * @param data Datos de la respuesta
     * @return ApiResponse con success=true
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Construye una respuesta exitosa sin datos.
     *
     * @param message Mensaje descriptivo
     * @return ApiResponse con success=true y data=null
     */
    public static <T> ApiResponse<T> ok(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Construye una respuesta de error con código de dominio.
     *
     * @param message Mensaje descriptivo del error
     * @param code Código de dominio (VALIDATION_ERROR, UNAUTHORIZED, etc.)
     * @return ApiResponse con success=false
     */
    public static <T> ApiResponse<T> error(String message, String code) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .code(code)
                .build();
    }

    /**
     * Construye una respuesta de error de validación con errores por campo.
     *
     * @param message Mensaje descriptivo del error
     * @param fields Mapa de campo -> lista de errores de ese campo
     * @return ApiResponse con success=false y code=VALIDATION_ERROR
     */
    public static <T> ApiResponse<T> validationError(String message, java.util.Map<String, java.util.List<String>> fields) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .code("VALIDATION_ERROR")
                .fields(fields)
                .build();
    }
}
