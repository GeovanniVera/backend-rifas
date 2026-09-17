package com.semillatecnologica.backend.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.semillatecnologica.backend.modules.auth.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO de salida con los datos del usuario autenticado.
 *
 * <p>Se utiliza en las respuestas de login, refresh y /auth/me.
 * roles = etiquetas, permissions = fuente de verdad para el front.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos del usuario autenticado")
public class UserResponse {

    @Schema(description = "ID único del usuario", example = "d1b513d1-795a-42be-9bc4-b96506588579")
    private String id;

    @Schema(description = "Email del usuario", example = "admin@test.com")
    private String email;

    @Schema(description = "Nombre completo del usuario", example = "Admin Test")
    private String name;

    @Schema(description = "Roles asignados (etiquetas)", example = "[\"admin\"]")
    private List<String> roles;

    @Schema(description = "Permisos efectivos del usuario (unión de permisos de todos sus roles). Usar para renderizar UI.", example = "[\"users.read\", \"users.write\"]")
    private List<String> permissions;

    @Schema(description = "Indica si el correo está verificado", example = "true")
    @JsonProperty("isVerified")
    private boolean isVerified;

    @Schema(description = "URL de la foto de perfil", example = "/api/files/xxx.jpg")
    private String photoUrl;

    /**
     * Mapea una entidad User a su DTO de respuesta.
     *
     * @param user Entidad User con roles y permisos cargados
     * @return UserResponse con los datos del usuario
     */
    public static UserResponse fromEntity(User user) {
        return fromEntity(user, null);
    }

    /**
     * Mapea una entidad User a su DTO de respuesta con foto de perfil.
     *
     * @param user Entidad User con roles y permisos cargados
     * @param photoUrl URL de la foto de perfil (nullable)
     * @return UserResponse con los datos del usuario
     */
    public static UserResponse fromEntity(User user, String photoUrl) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        Set<String> permissionNames = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .roles(List.copyOf(roleNames))
                .permissions(List.copyOf(permissionNames))
                .isVerified(user.getIsVerified())
                .photoUrl(photoUrl)
                .build();
    }
}
