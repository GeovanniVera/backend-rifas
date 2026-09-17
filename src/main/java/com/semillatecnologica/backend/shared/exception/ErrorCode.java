package com.semillatecnologica.backend.shared.exception;

/**
 * Códigos de dominio de error del sistema.
 *
 * <p>Cada código corresponde a un tipo específico de error de negocio.
 * El frontend puede usar estos códigos para mostrar mensajes específicos
 * o ejecutar lógica condicional.</p>
 */
public enum ErrorCode {

    /**
     * Campos ausentes, formato incorrecto o petición mal formada.
     */
    VALIDATION_ERROR,

    /**
     * OTP incorrecto. No revela si la cuenta existe.
     */
    INVALID_OTP,

    /**
     * Credenciales incorrectas o access token ausente, inválido o expirado.
     */
    UNAUTHORIZED,

    /**
     * Credencial de sesión expirada.
     */
    TOKEN_EXPIRED,

    /**
     * Usuario autenticado sin el permiso requerido o cuenta suspendida.
     */
    FORBIDDEN,

    /**
     * Recurso inexistente cuando revelar su ausencia sea seguro.
     */
    NOT_FOUND,

    /**
     * Conflicto de estado en operaciones donde sea seguro revelarlo.
     */
    CONFLICT,

    /**
     * Token opaco de recuperación consumido o expirado.
     */
    RESET_TOKEN_EXPIRED,

    /**
     * Datos sintácticamente válidos que no cumplen una regla de negocio.
     */
    UNPROCESSABLE_ENTITY,

    /**
     * Se superó el límite de solicitudes o intentos.
     */
    RATE_LIMITED,

    /**
     * Fallo interno con mensaje opaco.
     */
    INTERNAL_ERROR,

    /**
     * Configuración interna incompleta (rol, permiso o recurso base ausente).
     */
    INTERNAL_CONFIG_ERROR
}
