package com.semillatecnologica.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de entrada para restablecer la contraseña con un token de recuperación.
 *
 * <p>El token es opaco, de un solo uso y de vigencia corta.</p>
 */
@Data
public class ResetPasswordRequest {

    /**
     * Token opaco de recuperación.
     */
    @NotBlank(message = "El token es obligatorio")
    private String token;

    /**
     * Nueva contraseña del usuario.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    private String password;
}
