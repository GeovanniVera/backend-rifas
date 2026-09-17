package com.semillatecnologica.backend.modules.admin.service;

import com.semillatecnologica.backend.modules.admin.dto.AssignRolesRequest;
import com.semillatecnologica.backend.modules.admin.dto.UserResponse;
import com.semillatecnologica.backend.modules.auth.model.Role;
import com.semillatecnologica.backend.modules.auth.model.User;
import com.semillatecnologica.backend.modules.auth.repository.RefreshTokenRepository;
import com.semillatecnologica.backend.modules.auth.repository.RoleRepository;
import com.semillatecnologica.backend.modules.auth.repository.UserRepository;
import com.semillatecnologica.backend.shared.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de asignación de roles a usuarios.
 *
 * <p>Maneja la asignación y remoción de roles, con invalidación
 * de sesiones cuando cambia la autoridad del usuario.</p>
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    /**
     * Asigna roles a un usuario, reemplazando los existentes.
     *
     * <p>Invalida todas las sesiones del usuario después del cambio.</p>
     *
     * @param userId ID del usuario
     * @param roleIds IDs de los roles a asignar
     * @return Datos del usuario con los nuevos roles
     */
    @Transactional
    public UserResponse assignRoles(String userId, Set<String> roleIds, HttpServletRequest request) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new NotFoundException("Usuario"));

        // Capturar roles antes del cambio
        Set<String> beforeRoles = user.getRoles().stream()
                .map(Role::getName).collect(Collectors.toSet());

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));

        user.getRoles().clear();
        roles.forEach(user::addRole);

        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());

        // Capturar roles después del cambio
        Set<String> afterRoles = user.getRoles().stream()
                .map(Role::getName).collect(Collectors.toSet());

        auditService.log("ROLE_CHANGED", "USER", userId, userId,
                Map.of("roles", beforeRoles),
                Map.of("roles", afterRoles),
                request);

        return toResponse(user);
    }

    /**
     * Remueve un rol específico de un usuario.
     *
     * <p>Invalida todas las sesiones del usuario después del cambio.</p>
     *
     * @param userId ID del usuario
     * @param roleId ID del rol a remover
     * @return Datos del usuario sin el rol
     */
    @Transactional
    public UserResponse removeRole(String userId, String roleId, HttpServletRequest request) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new NotFoundException("Usuario"));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Rol"));

        // Capturar roles antes del cambio
        Set<String> beforeRoles = user.getRoles().stream()
                .map(Role::getName).collect(Collectors.toSet());

        user.removeRole(role);

        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());

        // Capturar roles después del cambio
        Set<String> afterRoles = user.getRoles().stream()
                .map(Role::getName).collect(Collectors.toSet());

        auditService.log("ROLE_CHANGED", "USER", userId, userId,
                Map.of("roles", beforeRoles),
                Map.of("roles", afterRoles),
                request);

        return toResponse(user);
    }

    /**
     * Convierte una entidad User a su DTO de respuesta.
     *
     * @param user Entidad a convertir
     * @return DTO con los datos del usuario
     */
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()))
                .isVerified(user.getIsVerified())
                .suspended(user.getSuspended())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
