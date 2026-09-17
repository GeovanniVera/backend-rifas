package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción lanzada cuando un usuario no está autenticado
 * o sus credenciales son incorrectas.
 */
public class UnauthorizedException extends DomainException {

    /**
     * Construye una excepción de autorización con mensaje genérico.
     */
    public UnauthorizedException() {
        super("Credenciales incorrectas o token inválido", ErrorCode.UNAUTHORIZED);
    }

    /**
     * Construye una excepción de autorización con mensaje personalizado.
     *
     * @param message Mensaje descriptivo del error
     */
    public UnauthorizedException(String message) {
        super(message, ErrorCode.UNAUTHORIZED);
    }
}
