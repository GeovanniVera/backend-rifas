package com.semillatecnologica.backend.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un usuario del sistema.
 *
 * <p>Almacena la identidad básica, credenciales y estado de verificación.
 * Los roles y permisos se resuelven mediante las tablas de relación.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "terms_version", length = 20)
    private String termsVersion;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean suspended = false;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "photo_file_id", length = 36)
    private String photoFileId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Agrega un rol al usuario.
     *
     * @param role Rol a agregar
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /**
     * Elimina un rol del usuario.
     *
     * @param role Rol a eliminar
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }
}
