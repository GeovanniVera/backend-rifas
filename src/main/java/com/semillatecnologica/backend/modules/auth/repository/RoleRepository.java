package com.semillatecnologica.backend.modules.auth.repository;

import com.semillatecnologica.backend.modules.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

/**
 * Repositorio de roles del sistema.
 *
 * <p>Proporciona consultas para resolución de roles y permisos
 * asignados a usuarios.</p>
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    /**
     * Busca un rol por su nombre.
     *
     * @param name Nombre del rol (ej: admin, editor, viewer)
     * @return El rol si existe, vacío si no
     */
    Optional<Role> findByName(String name);

    /**
     * Verifica si existe un rol con el nombre dado.
     *
     * @param name Nombre a verificar
     * @return true si existe un rol con ese nombre
     */
    boolean existsByName(String name);

    /**
     * Carga los roles de un usuario con sus permisos.
     *
     * @param userId ID del usuario
     * @return Conjunto de roles con permisos cargados
     */
    @Query("SELECT DISTINCT r FROM Role r " +
           "JOIN FETCH r.permissions " +
           "WHERE r.id IN " +
           "(SELECT r2.id FROM User u JOIN u.roles r2 WHERE u.id = :userId)")
    Set<Role> findRolesWithPermissionsByUserId(@Param("userId") String userId);
}
