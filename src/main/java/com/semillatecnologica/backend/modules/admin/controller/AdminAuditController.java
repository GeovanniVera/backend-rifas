package com.semillatecnologica.backend.modules.admin.controller;

import com.semillatecnologica.backend.modules.admin.dto.AuditLogResponse;
import com.semillatecnologica.backend.modules.admin.model.AuditLog;
import com.semillatecnologica.backend.modules.admin.repository.AuditLogRepository;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controlador de consulta de auditoría.
 *
 * <p>Los registros son inmutables — solo se pueden consultar, no modificar.</p>
 *
 * <p>Alcance por permisos:
 * <ul>
 *   <li>{@code audit.read} → ve todos los eventos</li>
 *   <li>{@code audit.read-mine} → ve solo sus propios eventos (actorId = usuario actual)</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin - Auditoría", description = "Consulta de eventos de auditoría del sistema")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    /**
     * Lista eventos de auditoría con filtros opcionales.
     *
     * <p>Si el usuario tiene {@code audit.read} ve todos los eventos.
     * Si solo tiene {@code audit.read-mine}, se fuerza el filtro
     * {@code actorId = usuario actual}.</p>
     *
     * @param action Filtrar por tipo de acción
     * @param actorId Filtrar por ID del actor (ignorado si solo tiene audit.read-mine)
     * @param entityType Filtrar por tipo de entidad
     * @param entityId Filtrar por ID de la entidad
     * @param from Fecha inicio (ISO 8601)
     * @param to Fecha fin (ISO 8601)
     * @param pageable Parámetros de paginación
     * @param authentication Usuario autenticado
     * @return Página de eventos de auditoría
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('audit.read', 'audit.read-mine')")
    @Operation(summary = "Consultar auditoría",
               description = "Requiere `audit.read` (todo) o `audit.read-mine` (solo lo propio).")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> listAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable,
            Authentication authentication) {

        boolean canSeeAll = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("audit.read"));

        // Si solo puede ver lo propio, forzar actorId = usuario actual
        String effectiveActorId = actorId;
        if (!canSeeAll) {
            effectiveActorId = authentication.getName();
        }

        Page<AuditLog> logs = auditLogRepository.findWithFilters(
                action, effectiveActorId, entityType, entityId, from, to, pageable);

        Page<AuditLogResponse> response = logs.map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.ok("Eventos de auditoría", response));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .requestId(log.getRequestId())
                .actorId(log.getActorId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .before(log.getBeforeJson())
                .after(log.getAfterJson())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}