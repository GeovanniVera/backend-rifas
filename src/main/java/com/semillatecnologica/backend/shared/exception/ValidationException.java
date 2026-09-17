package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción lanzada cuando los datos de entrada no pasan la validación.
 *
 * <p>Se lanza en controladores o servicios cuando los campos obligatorios
 * están ausentes o tienen un formato incorrecto.</p>
 */
public class ValidationException extends DomainException {

    /**
     * Construye una excepción de validación con mensaje descriptivo.
     *
     * @param message Mensaje que describe qué campo es inválido
     */
    public ValidationException(String message) {
        super(message, ErrorCode.VALIDATION_ERROR);
    }
}
