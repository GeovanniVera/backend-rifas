package com.semillatecnologica.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de entrada para verificar el correo electrónico.
 *
 * <p>El token es opaco, de un solo uso y se envía al correo
 * durante el registro.</p>
 */
@Data
public class VerifyEmailRequest {

    /**
     * Token de verificación recibido por correo.
     */
    @NotBlank(message = "El token es obligatorio")
    private String token;
}
