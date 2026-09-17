package com.semillatecnologica.backend.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de salida que contiene los tokens de sesión y datos del usuario.
 *
 * <p>Se devuelve en las respuestas de login y refresh.
 * El access token se incluye en el body; el refresh token en una cookie HttpOnly.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tokens de sesión y datos del usuario")
public class TokenResponse {

    @Schema(description = "Datos del usuario autenticado")
    private UserResponse user;

    @Schema(description = "Access token JWT de vida corta")
    private String accessToken;

    @Schema(description = "Tiempo de vida del access token en segundos", example = "900")
    private int expiresIn;
}
