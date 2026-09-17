package com.semillatecnologica.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO de entrada para verificar un OTP de recuperación.
 *
 * <p>El OTP debe tener el formato esperado (generalmente 6 dígitos).</p>
 */
@Data
public class VerifyOtpRequest {

    /**
     * Email del usuario (normalizado).
     */
    @NotBlank(message = "El correo es obligatorio")
    @jakarta.validation.constraints.Email(message = "El formato del correo es inválido")
    private String email;

    /**
     * Código OTP ingresado por el usuario.
     */
    @NotBlank(message = "El código OTP es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El código OTP debe tener 6 dígitos")
    private String otp;
}
