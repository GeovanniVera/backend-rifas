package com.semillatecnologica.backend.shared.exception;

/**
 * Excepción lanzada cuando un rol requerido no existe en el sistema.
 *
 * <p>Es un error de configuración interno, no de usuario.
 * Indica que falta sembrar el rol en la base de datos.</p>
 */
public class RoleNotFoundException extends DomainException {

    /**
     * Construye la excepción con el nombre del rol faltante.
     *
     * @param roleName Nombre del rol que no se encontró
     */
    public RoleNotFoundException(String roleName) {
        super("Rol requerido no encontrado: " + roleName, ErrorCode.INTERNAL_CONFIG_ERROR);
    }
}
