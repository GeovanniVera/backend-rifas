package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción de rate limiting (429).
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}