package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción base del dominio del sistema.
 *
 * <p>Todas las excepciones de negocio deben heredar de esta clase
 * para permitir el manejo centralizado en el handler global.</p>
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode domainCode;

    /**
     * Construye una excepción de dominio con mensaje y código.
     *
     * @param message Mensaje descriptivo del error
     * @param domainCode Código de dominio del error
     */
    protected DomainException(String message, ErrorCode domainCode) {
        super(message);
        this.domainCode = domainCode;
    }

    /**
     * Construye una excepción de dominio con mensaje, código y causa original.
     *
     * @param message Mensaje descriptivo del error
     * @param domainCode Código de dominio del error
     * @param cause Excepción original que provocó este error
     */
    protected DomainException(String message, ErrorCode domainCode, Throwable cause) {
        super(message, cause);
        this.domainCode = domainCode;
    }

    /**
     * Devuelve el código de dominio del error.
     *
     * @return Código de dominio
     */
    public ErrorCode getDomainCode() {
        return domainCode;
    }
}
