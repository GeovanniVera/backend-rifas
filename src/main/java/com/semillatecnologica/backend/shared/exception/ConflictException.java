package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción lanzada cuando existe un conflicto de estado
 * en operaciones donde sea seguro revelarlo.
 *
 * <p>No se usa en el registro público para indicar que un correo ya existe.</p>
 */
public class ConflictException extends DomainException {

    /**
     * Construye una excepción de conflicto con mensaje descriptivo.
     *
     * @param message Mensaje que describe el conflicto
     */
    public ConflictException(String message) {
        super(message, ErrorCode.CONFLICT);
    }
}
