package com.semillatecnologica.backend.modules.admin.service;

import com.semillatecnologica.backend.modules.admin.model.AuditLog;
import com.semillatecnologica.backend.modules.admin.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Servicio de auditoría de negocio.
 *
 * <p>Registra eventos de seguridad y cumplimiento de forma inmutable.
 * Los registros se persisten de forma síncrona dentro de la misma
 * transacción que el evento de negocio.</p>
 *
 * <p>Configurable via {@code audit.enabled} en application.yaml.
 * Cuando está desactivado, no persiste ni loggea eventos.</p>
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT_LOG");

    private final AuditLogRepository auditLogRepository;

    @Value("${audit.enabled:true}")
    private boolean auditEnabled;

    /**
     * Registra un evento de auditoría.
     *
     * @param action Tipo de acción (LOGIN_SUCCEEDED, ROLE_CHANGED, etc.)
     * @param entityType Tipo de entidad (USER, ROLE, etc.)
     * @param entityId ID de la entidad afectada
     * @param actorId ID del usuario que realizó la acción
     * @param before Estado anterior (puede ser null)
     * @param after Estado nuevo (puede ser null)
     * @param request HTTP request para IP y User-Agent
     */
    @Transactional
    public void log(String action, String entityType, String entityId,
                    String actorId, Map<String, Object> before,
                    Map<String, Object> after, HttpServletRequest request) {
        if (!auditEnabled) return;

        String requestId = request != null
                ? request.getHeader("X-Request-Id")
                : null;
        String ipAddress = request != null
                ? resolveIp(request)
                : null;
        String userAgent = request != null
                ? request.getHeader("User-Agent")
                : null;

        AuditLog entry = AuditLog.builder()
                .id(UUID.randomUUID().toString())
                .requestId(requestId)
                .actorId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeJson(before)
                .afterJson(after)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(entry);

        auditLog.info("action={} entity={}{} actor={} request={}",
                action,
                entityType != null ? entityType : "",
                entityId != null ? "/" + entityId : "",
                actorId != null ? actorId : "system",
                requestId != null ? requestId : "N/A");
    }

    /**
     * Registra un evento sin HTTP request (para operaciones internas).
     */
    @Transactional
    public void log(String action, String entityType, String entityId,
                    String actorId, Map<String, Object> before,
                    Map<String, Object> after) {
        log(action, entityType, entityId, actorId, before, after, null);
    }

    /**
     * Extrae la IP real del cliente, considerando proxies.
     */
    private String resolveIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
