package com.semillatecnologica.backend.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un rol del sistema.
 *
 * <p>Los roles agrupan permisos. Un usuario puede tener múltiples roles.
 * Los permisos efectivos son la unión de los permisos de todos los roles asignados.</p>
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Role {

    @Id
    @Column(length = 36)
    @EqualsAndHashCode.Include
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Permission> permissions = new HashSet<>();

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> users = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Agrega un permiso al rol.
     *
     * @param permission Permiso a agregar
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    /**
     * Elimina un permiso del rol.
     *
     * @param permission Permiso a eliminar
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }
}
