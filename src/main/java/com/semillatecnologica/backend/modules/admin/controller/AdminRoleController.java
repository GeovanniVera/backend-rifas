package com.semillatecnologica.backend.modules.admin.controller;

import com.semillatecnologica.backend.modules.admin.dto.*;
import com.semillatecnologica.backend.modules.admin.service.AdminRoleService;
import com.semillatecnologica.backend.modules.admin.service.AssignmentService;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Controlador administrativo de roles y asignación.
 *
 * <p>Expone endpoints para CRUD de roles, catálogo de permisos
 * y asignación de roles a usuarios.</p>
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Roles y Permisos", description = "Gestión de roles, permisos y asignación a usuarios")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;
    private final AssignmentService assignmentService;

    // ========== ROLES ==========

    /**
     * Lista todos los roles del sistema.
     *
     * @return Lista de roles
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('roles.read')")
    @Operation(summary = "Listar roles", description = "Requiere permiso `roles.read`")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
        List<RoleResponse> roles = adminRoleService.listRoles();
        return ResponseEntity.ok(ApiResponse.ok("Roles listados", roles));
    }

    /**
     * Obtiene los detalles de un rol.
     *
     * @param roleId ID del rol
     * @return Datos del rol
     */
    @GetMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('roles.read')")
    @Operation(summary = "Obtener rol", description = "Requiere permiso `roles.read`")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable String roleId) {
        RoleResponse role = adminRoleService.getRole(roleId);
        return ResponseEntity.ok(ApiResponse.ok("Rol encontrado", role));
    }

    /**
     * Crea un nuevo rol.
     *
     * @param request Datos del rol
     * @return Datos del rol creado
     */
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('roles.write')")
    @Operation(summary = "Crear rol", description = "Requiere permiso `roles.write`")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        RoleResponse role = adminRoleService.createRole(request);
        return ResponseEntity.ok(ApiResponse.ok("Rol creado correctamente", role));
    }

    /**
     * Actualiza un rol existente.
     *
     * @param roleId ID del rol
     * @param request Datos a actualizar
     * @return Datos del rol actualizado
     */
    @PutMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('roles.write')")
    @Operation(summary = "Actualizar rol", description = "Requiere permiso `roles.write`")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable String roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        RoleResponse role = adminRoleService.updateRole(roleId, request);
        return ResponseEntity.ok(ApiResponse.ok("Rol actualizado", role));
    }

    /**
     * Elimina un rol.
     *
     * <p>No permite eliminar roles asignados a usuarios.</p>
     *
     * @param roleId ID del rol
     * @return Mensaje de confirmación
     */
    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('roles.write')")
    @Operation(summary = "Eliminar rol", description = "No permite eliminar roles asignados a usuarios. Requiere permiso `roles.write`")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable String roleId) {
        adminRoleService.deleteRole(roleId);
        return ResponseEntity.ok(ApiResponse.ok("Rol eliminado correctamente"));
    }

    // ========== PERMISOS ==========

    /**
     * Lista todos los permisos del catálogo.
     *
     * <p>Los permisos son de solo lectura.</p>
     *
     * @return Lista de permisos
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('permissions.read')")
    @Operation(summary = "Listar permisos", description = "Catálogo de solo lectura. Requiere permiso `permissions.read`")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> listPermissions() {
        List<PermissionResponse> permissions = adminRoleService.listPermissions();
        return ResponseEntity.ok(ApiResponse.ok("Permisos listados", permissions));
    }

    // ========== ASIGNACIÓN DE ROLES ==========

    /**
     * Asigna roles a un usuario.
     *
     * <p>Reemplaza los roles existentes. Invalida todas las sesiones del usuario.</p>
     *
     * @param userId ID del usuario
     * @param request IDs de los roles a asignar
     * @return Datos del usuario con los nuevos roles
     */
    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('roles.assign')")
    @Operation(summary = "Asignar roles a usuario", description = "Reemplaza roles existentes e invalida sesiones. Requiere permiso `roles.assign`")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable String userId,
            @Valid @RequestBody AssignRolesRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        UserResponse user = assignmentService.assignRoles(userId, request.roleIds(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Roles asignados correctamente", user));
    }

    /**
     * Remueve un rol específico de un usuario.
     *
     * <p>Invalida todas las sesiones del usuario.</p>
     *
     * @param userId ID del usuario
     * @param roleId ID del rol a remover
     * @return Datos del usuario sin el rol
     */
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('roles.assign')")
    @Operation(summary = "Remover rol de usuario", description = "Invalida sesiones del usuario. Requiere permiso `roles.assign`")
    public ResponseEntity<ApiResponse<UserResponse>> removeRole(
            @PathVariable String userId,
            @PathVariable String roleId,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        UserResponse user = assignmentService.removeRole(userId, roleId, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Rol removido correctamente", user));
    }
}
