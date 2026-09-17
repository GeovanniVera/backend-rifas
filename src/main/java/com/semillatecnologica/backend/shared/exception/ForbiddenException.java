package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción lanzada cuando un usuario autenticado intenta acceder
 * a un recurso o ejecutar una operación para la cual no tiene permiso.
 */
public class ForbiddenException extends DomainException {

    /**
     * Construye una excepción de prohibido con mensaje genérico.
     */
    public ForbiddenException() {
        super("No tienes permiso para realizar esta operación", ErrorCode.FORBIDDEN);
    }

    /**
     * Construye una excepción de prohibido con mensaje personalizado.
     *
     * @param message Mensaje descriptivo del error
     */
    public ForbiddenException(String message) {
        super(message, ErrorCode.FORBIDDEN);
    }
}
