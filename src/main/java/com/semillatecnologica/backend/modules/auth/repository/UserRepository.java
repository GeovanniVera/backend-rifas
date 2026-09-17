package com.semillatecnologica.backend.modules.auth.repository;

import com.semillatecnologica.backend.modules.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de usuarios del sistema.
 *
 * <p>Proporciona consultas para autenticación, verificación de correo
 * y resolución de permisos efectivos.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Busca un usuario por su email normalizado.
     *
     * @param email Email del usuario (normalizado a minúsculas)
     * @return El usuario si existe, vacío si no
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el email dado.
     *
     * @param email Email a verificar
     * @return true si existe un usuario con ese email
     */
    boolean existsByEmail(String email);

    /**
     * Carga un usuario con sus roles y permisos para autenticación.
     *
     * @param email Email del usuario
     * @return El usuario con roles y permisos cargados, vacío si no existe
     */
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.email = :email")
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    /**
     * Carga un usuario por ID con sus roles y permisos.
     *
     * @param id ID del usuario
     * @return El usuario con roles y permisos cargados, vacío si no existe
     */
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithRolesAndPermissions(@Param("id") String id);
}
