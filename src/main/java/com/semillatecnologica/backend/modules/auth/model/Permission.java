package com.semillatecnologica.backend.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un permiso del sistema.
 *
 * <p>Los permisos siguen la convención recurso.acción (ej: users.read, roles.assign).
 * El backend debe comprobar el permiso requerido en cada caso de uso.</p>
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
