package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción cuando la cuenta está suspendida.
 *
 * <p>Se lanza durante la autenticación JWT para impedir que
 * un usuario suspendido use un access token aún válido.</p>
 */
public class SuspendedAccountException extends RuntimeException {

    public SuspendedAccountException(String message) {
        super(message);
    }
}