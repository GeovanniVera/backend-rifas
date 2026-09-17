package com.semillatecnologica.backend.modules.auth.service;

import com.semillatecnologica.backend.modules.auth.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interfaz del servicio de autenticación.
 *
 * <p>Define los contratos para login, registro, refresh, logout
 * y recuperación de contraseña.</p>
 */
public interface AuthService {

    /**
     * Autentica un usuario con email y contraseña.
     *
     * @param request Credenciales del usuario
     * @param httpRequest Solicitud HTTP para extraer IP y User-Agent
     * @param httpResponse Respuesta HTTP para setear la cookie del refresh token
     * @return TokenResponse con el access token y datos del usuario
     */
    TokenResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * Registra un nuevo usuario con privilegio mínimo.
     *
     * <p>Si el correo ya existe, no crea otra cuenta ni revela el conflicto.
     * La respuesta siempre es la misma para ambos caminos.</p>
     *
     * @param request Datos del usuario a registrar
     * @param httpRequest Solicitud HTTP para rate limiting por IP
     */
    void register(RegisterRequest request, HttpServletRequest httpRequest);

    /**
     * Rota el refresh token y emite un nuevo access token.
     *
     * @param httpRequest Solicitud HTTP con la cookie del refresh token
     * @param httpResponse Respuesta HTTP para rotar la cookie
     * @return TokenResponse con el nuevo access token
     */
    TokenResponse refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * Cierra la sesión actual revocando el refresh token.
     *
     * @param httpRequest Solicitud HTTP con la cookie del refresh token
     * @param httpResponse Respuesta HTTP para expirar la cookie
     */
    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * Cierra todas las sesiones activas del usuario autenticado.
     *
     * @param userId ID del usuario
     */
    void logoutAll(String userId);

    /**
     * Devuelve el estado actual del usuario autenticado.
     *
     * @param userId ID del usuario
     * @return UserResponse con datos y permisos actuales
     */
    UserResponse getMe(String userId);

    /**
     * Actualiza el perfil del usuario autenticado.
     *
     * @param userId ID del usuario
     * @param name Nombre a actualizar (nullable = sin cambio)
     * @param photo Foto de perfil (nullable = sin cambio)
     * @return UserResponse con datos actualizados
     */
    UserResponse updateMe(String userId, String name,
                          org.springframework.web.multipart.MultipartFile photo);

    /**
     * Solicita un OTP de recuperación de contraseña.
     *
     * @param request Email del usuario
     * @param httpRequest Solicitud HTTP para rate limiting por IP
     */
    void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest);

    /**
     * Verifica el OTP y devuelve un token de recuperación.
     *
     * @param request Email y OTP
     * @param httpRequest Solicitud HTTP para rate limiting por IP
     * @return OtpTokenResponse con el token de recuperación
     */
    OtpTokenResponse verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest);

    /**
     * Restablece la contraseña con un token de recuperación.
     *
     * @param request Token y nueva contraseña
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Verifica el correo electrónico con un token.
     *
     * @param request Token de verificación
     */
    void verifyEmail(VerifyEmailRequest request);

    /**
     * Reenvía el correo de verificación.
     *
     * <p>Respuesta opaca: no revela si el correo está registrado.</p>
     *
     * @param email Email del usuario
     */
    void resendVerification(String email);
}
