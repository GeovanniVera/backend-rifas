package com.semillatecnologica.backend.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * DTO de respuesta para permisos del catálogo.
 *
 * <p>Los permisos son de solo lectura; no se crean ni modifican desde la API.</p>
 */
@Builder
@Schema(description = "Permiso del catálogo (solo lectura)")
public record PermissionResponse(
    @Schema(description = "ID del permiso", example = "perm-users-read") String id,
    @Schema(description = "Nombre del permiso (resource.action)", example = "users.read") String name,
    @Schema(description = "Descripción del permiso", example = "Ver usuarios") String description
) {}
