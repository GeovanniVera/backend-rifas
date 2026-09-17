package com.semillatecnologica.backend.modules.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para actualizar un usuario.
 */
public record UpdateUserRequest(
    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    String name,

    @Email(message = "El formato del correo es inválido")
    @Size(max = 255, message = "El correo no puede exceder 255 caracteres")
    String email
) {}
