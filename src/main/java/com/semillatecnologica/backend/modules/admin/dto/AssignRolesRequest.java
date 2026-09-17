package com.semillatecnologica.backend.modules.admin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * DTO de entrada para asignar roles a un usuario.
 */
public record AssignRolesRequest(
    @NotEmpty(message = "Debe especificar al menos un rol")
    Set<String> roleIds
) {}
