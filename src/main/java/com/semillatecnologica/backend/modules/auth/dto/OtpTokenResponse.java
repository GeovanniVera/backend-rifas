package com.semillatecnologica.backend.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de salida que contiene el token de recuperación de contraseña.
 *
 * <p>Se devuelve después de verificar el OTP exitosamente.
 * El token es opaco, de un solo uso y de vigencia corta.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpTokenResponse {

    /**
     * Token opaco de recuperación de contraseña.
     */
    private String resetToken;
}
