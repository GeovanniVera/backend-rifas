package com.semillatecnologica.backend.modules.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO de respuesta con los datos de un usuario (vista administrativa).
 *
 * <p>Incluye campos de suspensión y timestamps que no están en el DTO de auth.</p>
 */
@Builder
@Schema(description = "Datos de un usuario (vista administrativa)")
public record UserResponse(
    @Schema(description = "ID único del usuario") String id,
    @Schema(description = "Email del usuario") String email,
    @Schema(description = "Nombre completo") String name,
    @Schema(description = "Roles asignados (nombres)") Set<String> roles,
    @JsonProperty("isVerified") @Schema(description = "Correo verificado") Boolean isVerified,
    @Schema(description = "Cuenta suspendida") Boolean suspended,
    @Schema(description = "Fecha de creación") LocalDateTime createdAt
) {}
