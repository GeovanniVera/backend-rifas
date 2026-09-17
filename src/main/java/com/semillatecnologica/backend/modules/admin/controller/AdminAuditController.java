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
 * <p>Endpoints separados por alcance (anti-IDOR):
 * <ul>
 *   <li>{@code GET /admin/audit-logs} → solo {@code audit.read}; ve todos los eventos
 *       y puede filtrar por {@code actorId} de cualquier usuario.</li>
 *   <li>{@code GET /admin/audit-logs/mine} → {@code audit.read} o {@code audit.read-mine};
 *       ve únicamente sus propios eventos. El {@code actorId} se fija al usuario
 *       autenticado y no acepta un actor externo.</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin - Auditoría", description = "Consulta de eventos de auditoría del sistema")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    /**
     * Lista todos los eventos de auditoría (solo {@code audit.read}).
     *
     * <p>Permite filtrar por {@code actorId} de cualquier usuario: quien llega
     * a este endpoint ya tiene permiso para ver todo.</p>
     *
     * @param action Filtrar por tipo de acción
     * @param actorId Filtrar por ID del actor
     * @param entityType Filtrar por tipo de entidad
     * @param entityId Filtrar por ID de la entidad
     * @param from Fecha inicio (ISO 8601)
     * @param to Fecha fin (ISO 8601)
     * @param pageable Parámetros de paginación
     * @return Página de eventos de auditoría
     */
    @GetMapping
    @PreAuthorize("hasAuthority('audit.read')")
    @Operation(summary = "Consultar auditoría completa",
               description = "Requiere `audit.read`. Ve todos los eventos y puede filtrar por actor.")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> listAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {

        Page<AuditLog> logs = auditLogRepository.findWithFilters(
                action, actorId, entityType, entityId, from, to, pageable);

        Page<AuditLogResponse> response = logs.map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.ok("Eventos de auditoría", response));
    }

    /**
     * Lista únicamente los eventos del usuario autenticado.
     *
     * <p>El {@code actorId} se fija al usuario actual: no acepta un actor
     * externo, evitando IDOR incluso si el cliente lo intenta.</p>
     *
     * @param action Filtrar por tipo de acción
     * @param entityType Filtrar por tipo de entidad
     * @param entityId Filtrar por ID de la entidad
     * @param from Fecha inicio (ISO 8601)
     * @param to Fecha fin (ISO 8601)
     * @param pageable Parámetros de paginación
     * @param authentication Usuario autenticado
     * @return Página de eventos del usuario actual
     */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyAuthority('audit.read', 'audit.read-mine')")
    @Operation(summary = "Consultar mi actividad",
               description = "Requiere `audit.read` o `audit.read-mine`. Ve solo sus propios eventos.")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> listMyAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable,
            Authentication authentication) {

        Page<AuditLog> logs = auditLogRepository.findWithFilters(
                action, authentication.getName(), entityType, entityId, from, to, pageable);

        Page<AuditLogResponse> response = logs.map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.ok("Mi actividad", response));
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