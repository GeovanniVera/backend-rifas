package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no existe.
 *
 * <p>Solo se utiliza cuando revelar la ausencia del recurso es seguro.
 * No se usa para enumerar cuentas.</p>
 */
public class NotFoundException extends DomainException {

    /**
     * Construye una excepción de no encontrado con nombre del recurso.
     *
     * @param resourceName Nombre del recurso que no se encontró
     */
    public NotFoundException(String resourceName) {
        super(resourceName + " no encontrado", ErrorCode.NOT_FOUND);
    }

    /**
     * Construye una excepción de no encontrado con mensaje personalizado.
     *
     * @param message Mensaje descriptivo del error
     */
    public NotFoundException(String message, boolean customMessage) {
        super(message, ErrorCode.NOT_FOUND);
    }
}
