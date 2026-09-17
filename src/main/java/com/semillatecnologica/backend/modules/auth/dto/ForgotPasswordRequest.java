package com.semillatecnologica.backend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de entrada para solicitar recuperación de contraseña.
 *
 * <p>El email se valida y normaliza. La respuesta siempre es opaca
 * para no revelar si la cuenta existe.</p>
 */
@Data
public class ForgotPasswordRequest {

    /**
     * Email del usuario que solicita la recuperación.
     */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String email;
}
