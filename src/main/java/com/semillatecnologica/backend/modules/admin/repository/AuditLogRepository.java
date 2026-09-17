package com.semillatecnologica.backend.modules.admin.repository;

import com.semillatecnologica.backend.modules.admin.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repositorio de eventos de auditoría.
 *
 * <p>Proporciona consultas filtrables para la consulta administrativa.
 * Los registros son inmutables — no hay update ni delete desde la API.</p>
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    /**
     * Busca eventos con filtros opcionales usando query nativa.
     *
     * <p>Hibernate 6 / PostgreSQL no infiere correctamente el tipo
     * de parámetros null en JPQL, por eso se usa SQL nativo con COALESCE.</p>
     */
    @Query(value = "SELECT al.* FROM audit_logs al WHERE " +
           "COALESCE(:action, al.action) = al.action AND " +
           "COALESCE(:actorId, al.actor_id) = al.actor_id AND " +
           "COALESCE(:entityType, al.entity_type) = al.entity_type AND " +
           "COALESCE(:entityId, al.entity_id) = al.entity_id AND " +
           "al.created_at >= COALESCE(:from, al.created_at) AND " +
           "al.created_at <= COALESCE(:to, al.created_at) " +
           "ORDER BY al.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM audit_logs al WHERE " +
           "COALESCE(:action, al.action) = al.action AND " +
           "COALESCE(:actorId, al.actor_id) = al.actor_id AND " +
           "COALESCE(:entityType, al.entity_type) = al.entity_type AND " +
           "COALESCE(:entityId, al.entity_id) = al.entity_id AND " +
           "al.created_at >= COALESCE(:from, al.created_at) AND " +
           "al.created_at <= COALESCE(:to, al.created_at)",
           nativeQuery = true)
    Page<AuditLog> findWithFilters(
            @Param("action") String action,
            @Param("actorId") String actorId,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
