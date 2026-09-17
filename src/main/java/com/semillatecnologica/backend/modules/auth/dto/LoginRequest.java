package com.semillatecnologica.backend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de entrada para el inicio de sesión.
 *
 * <p>Contiene las credenciales del usuario: email y contraseña.</p>
 */
@Data
public class LoginRequest {

    /**
     * Email del usuario (normalizado a minúsculas).
     */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String email;

    /**
     * Contraseña del usuario en texto plano.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 1, max = 128, message = "La contraseña no puede estar vacía")
    private String password;
}
