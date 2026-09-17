package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción lanzada cuando los datos no cumplen una regla de negocio.
 *
 * <p>Equivale a HTTP 422 Unprocessable Entity.</p>
 */
public class UnprocessableEntityException extends DomainException {

    /**
     * Construye la excepción con un mensaje descriptivo.
     *
     * @param message Mensaje que describe la violación de regla
     */
    public UnprocessableEntityException(String message) {
        super(message, ErrorCode.UNPROCESSABLE_ENTITY);
    }
}
