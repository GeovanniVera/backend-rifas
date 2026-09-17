package com.semillatecnologica.backend.modules.auth.repository;

import com.semillatecnologica.backend.modules.auth.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de permisos del sistema.
 *
 * <p>Almacena el catálogo de permisos en formato recurso.acción.</p>
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    /**
     * Busca un permiso por su nombre.
     *
     * @param name Nombre del permiso (ej: users.read, roles.assign)
     * @return El permiso si existe, vacío si no
     */
    Optional<Permission> findByName(String name);

    /**
     * Verifica si existe un permiso con el nombre dado.
     *
     * @param name Nombre a verificar
     * @return true si existe un permiso con ese nombre
     */
    boolean existsByName(String name);
}
