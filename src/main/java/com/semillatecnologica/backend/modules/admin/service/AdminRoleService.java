package com.semillatecnologica.backend.modules.admin.service;

import com.semillatecnologica.backend.modules.admin.dto.*;
import com.semillatecnologica.backend.modules.auth.model.Permission;
import com.semillatecnologica.backend.modules.auth.model.Role;
import com.semillatecnologica.backend.modules.auth.repository.PermissionRepository;
import com.semillatecnologica.backend.modules.auth.repository.RefreshTokenRepository;
import com.semillatecnologica.backend.modules.auth.repository.RoleRepository;
import com.semillatecnologica.backend.shared.exception.ConflictException;
import com.semillatecnologica.backend.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio administrativo de gestión de roles.
 *
 * <p>Expone operaciones CRUD de roles y el catálogo de permisos.
 * Los permisos son de solo lectura y se definen en migraciones SQL.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Lista todos los roles del sistema.
     *
     * @return Lista de roles
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Obtiene los detalles de un rol por ID.
     *
     * @param roleId ID del rol
     * @return Datos del rol
     */
    @Transactional(readOnly = true)
    public RoleResponse getRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Rol"));
        return toResponse(role);
    }

    /**
     * Crea un nuevo rol con permisos.
     *
     * @param request Datos del rol a crear
     * @return Datos del rol creado
     */
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String normalizedName = request.name().toLowerCase().trim();

        if (roleRepository.findByName(normalizedName).isPresent()) {
            throw new ConflictException("Ya existe un rol con ese nombre");
        }

        Role role = Role.builder()
                .id(UUID.randomUUID().toString())
                .name(normalizedName)
                .description(request.description())
                .permissions(new HashSet<>())
                .build();

        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));
            permissions.forEach(role::addPermission);
        }

        roleRepository.save(role);
        return toResponse(role);
    }

    /**
     * Actualiza un rol existente.
     *
     * @param roleId ID del rol
     * @param request Datos a actualizar
     * @return Datos del rol actualizado
     */
    @Transactional
    public RoleResponse updateRole(String roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Rol"));

        if (request.name() != null) {
            String normalizedName = request.name().toLowerCase().trim();
            if (!normalizedName.equals(role.getName()) &&
                roleRepository.findByName(normalizedName).isPresent()) {
                throw new ConflictException("Ya existe un rol con ese nombre");
            }
            role.setName(normalizedName);
        }

        if (request.description() != null) {
            role.setDescription(request.description());
        }

        if (request.permissionIds() != null) {
            role.getPermissions().clear();
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));
            permissions.forEach(role::addPermission);
        }

        return toResponse(role);
    }

    /**
     * Elimina un rol del sistema.
     *
     * <p>No permite eliminar roles que estén asignados a usuarios.</p>
     *
     * @param roleId ID del rol
     */
    @Transactional
    public void deleteRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Rol"));

        if (!role.getUsers().isEmpty()) {
            throw new ConflictException("No se puede eliminar un rol asignado a usuarios");
        }

        roleRepository.delete(role);
    }

    /**
     * Lista todos los permisos del catálogo.
     *
     * <p>Los permisos son de solo lectura.</p>
     *
     * @return Lista de permisos
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    /**
     * Asigna roles a un usuario.
     *
     * @param userId ID del usuario
     * @param roleIds IDs de los roles a asignar
     */
    @Transactional
    public void assignRoles(String userId, Set<String> roleIds) {
        // La asignación se implementa en AdminUserService
        // Este método es para uso interno
    }

    /**
     * Convierte una entidad Role a su DTO de respuesta.
     *
     * @param role Entidad a convertir
     * @return DTO con los datos del rol
     */
    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(role.getPermissions().stream()
                        .map(this::toPermissionResponse)
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Convierte una entidad Permission a su DTO de respuesta.
     *
     * @param permission Entidad a convertir
     * @return DTO con los datos del permiso
     */
    private PermissionResponse toPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }
}
