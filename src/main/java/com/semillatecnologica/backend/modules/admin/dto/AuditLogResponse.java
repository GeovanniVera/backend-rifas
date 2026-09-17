package com.semillatecnologica.backend.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de respuesta para eventos de auditoría.
 */
@Builder
@Schema(description = "Evento de auditoría")
public record AuditLogResponse(
    @Schema(description = "ID del evento") String id,
    @Schema(description = "ID de correlación del request") String requestId,
    @Schema(description = "ID del usuario que realizó la acción") String actorId,
    @Schema(description = "Tipo de acción", example = "ROLE_CHANGED") String action,
    @Schema(description = "Tipo de entidad afectada", example = "USER") String entityType,
    @Schema(description = "ID de la entidad afectada") String entityId,
    @Schema(description = "Estado anterior") Map<String, Object> before,
    @Schema(description = "Estado nuevo") Map<String, Object> after,
    @Schema(description = "Dirección IP del cliente") String ipAddress,
    @Schema(description = "User-Agent del cliente") String userAgent,
    @Schema(description = "Fecha y hora del evento") LocalDateTime createdAt
) {}
