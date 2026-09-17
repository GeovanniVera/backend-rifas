package com.semillatecnologica.backend.modules.admin.controller;

import com.semillatecnologica.backend.modules.admin.dto.*;
import com.semillatecnologica.backend.modules.admin.service.AdminUserService;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador administrativo de usuarios.
 *
 * <p>Expone endpoints para CRUD de usuarios, suspensión y reactivación.
 * Todos los endpoints requieren permisos específicos.</p>
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Usuarios", description = "Gestión administrativa de usuarios del sistema")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Lista todos los usuarios con paginación.
     *
     * @param pageable Parámetros de paginación
     * @return Página de usuarios
     */
    @GetMapping
    @PreAuthorize("hasAuthority('users.read')")
    @Operation(summary = "Listar usuarios", description = "Requiere permiso `users.read`")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(Pageable pageable) {
        Page<UserResponse> users = adminUserService.listUsers(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Usuarios listados", users));
    }

    /**
     * Obtiene los detalles de un usuario.
     *
     * @param userId ID del usuario
     * @return Datos del usuario
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('users.read')")
    @Operation(summary = "Obtener usuario", description = "Requiere permiso `users.read`")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String userId) {
        UserResponse user = adminUserService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("Usuario encontrado", user));
    }

    /**
     * Suspende la cuenta de un usuario.
     *
     * <p>Invalida todas las sesiones activas del usuario.</p>
     *
     * @param userId ID del usuario
     * @return Datos del usuario suspendido
     */
    @PostMapping("/{userId}/suspend")
    @PreAuthorize("hasAuthority('users.write')")
    @Operation(summary = "Suspender usuario", description = "Invalida todas las sesiones activas. Requiere permiso `users.write`")
    public ResponseEntity<ApiResponse<UserResponse>> suspend(
            @PathVariable String userId,
            jakarta.servlet.http.HttpServletRequest request) {
        UserResponse user = adminUserService.suspend(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Usuario suspendido", user));
    }

    /**
     * Reactiva la cuenta de un usuario suspendido.
     *
     * @param userId ID del usuario
     * @return Datos del usuario reactivado
     */
    @PostMapping("/{userId}/reactivate")
    @PreAuthorize("hasAuthority('users.write')")
    @Operation(summary = "Reactivar usuario", description = "Requiere permiso `users.write`")
    public ResponseEntity<ApiResponse<UserResponse>> reactivate(
            @PathVariable String userId,
            jakarta.servlet.http.HttpServletRequest request) {
        UserResponse user = adminUserService.reactivate(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Usuario reactivado", user));
    }
}
