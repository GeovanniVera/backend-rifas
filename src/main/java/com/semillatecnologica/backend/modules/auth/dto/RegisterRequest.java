package com.semillatecnologica.backend.modules.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de entrada para el registro de usuarios.
 *
 * <p>Contiene los datos mínimos requeridos para crear una cuenta.
 * El campo acceptedTerms es obligatorio y debe ser true.</p>
 */
@Data
public class RegisterRequest {

    /**
     * Nombre completo del usuario.
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 1, max = 255, message = "El nombre no puede exceder 255 caracteres")
    private String name;

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
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    private String password;

    /**
     * Indica si el usuario aceptó los términos y condiciones.
     */
    @AssertTrue(message = "Debes aceptar los términos y condiciones")
    private boolean acceptedTerms;
}
