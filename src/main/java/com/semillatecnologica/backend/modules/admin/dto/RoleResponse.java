package com.semillatecnologica.backend.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Set;

/**
 * DTO de respuesta para creación/actualización de roles.
 *
 * <p>Incluye los permisos asignados al rol.</p>
 */
@Builder
@Schema(description = "Datos de un rol con sus permisos")
public record RoleResponse(
    @Schema(description = "ID único del rol") String id,
    @Schema(description = "Nombre del rol (slug)") String name,
    @Schema(description = "Descripción del rol") String description,
    @Schema(description = "Permisos asignados al rol") Set<PermissionResponse> permissions
) {}
