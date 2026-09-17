package com.semillatecnologica.backend.modules.admin.service;

import com.semillatecnologica.backend.modules.admin.dto.*;
import com.semillatecnologica.backend.modules.auth.model.User;
import com.semillatecnologica.backend.modules.auth.repository.RefreshTokenRepository;
import com.semillatecnologica.backend.modules.auth.repository.UserRepository;
import com.semillatecnologica.backend.shared.exception.NotFoundException;
import com.semillatecnologica.backend.shared.exception.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Servicio administrativo de gestión de usuarios.
 *
 * <p>Expone operaciones CRUD, suspensión y reactivación de cuentas.
 * Todas las operaciones de escritura invalidan las sesiones afectadas.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    /**
     * Lista todos los usuarios con paginación.
     *
     * @param pageable Parámetros de paginación
     * @return Página de usuarios
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    /**
     * Obtiene los detalles de un usuario por ID.
     *
     * @param userId ID del usuario
     * @return Datos del usuario
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(String userId) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new NotFoundException("Usuario"));
        return toResponse(user);
    }

    /**
     * Suspende la cuenta de un usuario.
     *
     * <p>Invalida todas las sesiones activas del usuario.</p>
     *
     * @param userId ID del usuario
     * @return Datos del usuario suspendido
     */
    @Transactional
    public UserResponse suspend(String userId, HttpServletRequest request) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new NotFoundException("Usuario"));

        Map<String, Object> before = Map.of("suspended", user.getSuspended() != null ? user.getSuspended() : false);

        user.setSuspended(true);
        user.setSuspendedAt(LocalDateTime.now());

        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());

        auditService.log("ACCOUNT_SUSPENDED", "USER", userId, userId,
                before, Map.of("suspended", true), request);

        return toResponse(user);
    }

    /**
     * Reactiva la cuenta de un usuario suspendido.
     *
     * @param userId ID del usuario
     * @return Datos del usuario reactivado
     */
    @Transactional
    public UserResponse reactivate(String userId, HttpServletRequest request) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new NotFoundException("Usuario"));

        Map<String, Object> before = Map.of("suspended", user.getSuspended() != null ? user.getSuspended() : false);

        user.setSuspended(false);
        user.setSuspendedAt(null);

        auditService.log("ACCOUNT_REACTIVATED", "USER", userId, userId,
                before, Map.of("suspended", false), request);

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
                        .collect(java.util.stream.Collectors.toSet()))
                .isVerified(user.getIsVerified())
                .suspended(user.getSuspended())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
