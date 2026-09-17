package com.semillatecnologica.backend.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * DTO de entrada para crear un rol.
 */
public record CreateRoleRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String name,

    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    String description,

    Set<String> permissionIds
) {}
