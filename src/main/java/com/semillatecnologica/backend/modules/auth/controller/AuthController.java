package com.semillatecnologica.backend.modules.auth.controller;

import com.semillatecnologica.backend.modules.auth.dto.*;
import com.semillatecnologica.backend.modules.auth.service.AuthService;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación.
 *
 * <p>Expone los endpoints públicos y protegidos del módulo de autenticación.
 * Los endpoints públicos no requieren autenticación; los protegidos
 * validan el access token JWT.</p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Inicia sesión con email y contraseña.
     *
     * <p>Devuelve el access token en el body y el refresh token en una cookie HttpOnly.</p>
     *
     * @param request Credenciales del usuario
     * @param httpRequest Solicitud HTTP
     * @param httpResponse Respuesta HTTP
     * @return TokenResponse con el access token y datos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        TokenResponse tokenResponse = authService.login(request, httpRequest, httpResponse);

        return ResponseEntity.ok(ApiResponse.ok("Inicio de sesión exitoso", tokenResponse));
    }

    /**
     * Registra un nuevo usuario.
     *
     * <p>La respuesta es opaca: no revela si el correo ya existe.
     * Si el correo es válido, el usuario recibirá instrucciones para verificar la cuenta.</p>
     *
     * @param request Datos del usuario a registrar
     * @return Mensaje opaco
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        authService.register(request, httpRequest);

        return ResponseEntity.ok(ApiResponse.ok(
                "Si la dirección puede utilizarse, recibirás instrucciones para verificar la cuenta."
        ));
    }

    /**
     * Rota el refresh token y emite un nuevo access token.
     *
     * <p>No recibe body. El refresh token viene en la cookie HttpOnly.</p>
     *
     * @param httpRequest Solicitud HTTP
     * @param httpResponse Respuesta HTTP
     * @return TokenResponse con el nuevo access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        TokenResponse tokenResponse = authService.refresh(httpRequest, httpResponse);

        return ResponseEntity.ok(ApiResponse.ok("Token renovado", tokenResponse));
    }

    /**
     * Cierra la sesión actual.
     *
     * <p>No recibe body. El refresh token viene en la cookie HttpOnly.
     * Devuelve 200 OK aunque la cookie falte, el token haya expirado
     * o la sesión ya esté revocada.</p>
     *
     * @param httpRequest Solicitud HTTP
     * @param httpResponse Respuesta HTTP
     * @return Mensaje de confirmación
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        authService.logout(httpRequest, httpResponse);

        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada correctamente"));
    }

    /**
     * Cierra todas las sesiones activas del usuario.
     *
     * @param authentication Contexto de autenticación del usuario
     * @return Mensaje de confirmación
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(Authentication authentication) {
        String userId = authentication.getName();
        authService.logoutAll(userId);

        return ResponseEntity.ok(ApiResponse.ok("Todas las sesiones han sido cerradas"));
    }

    /**
     * Devuelve el estado actual del usuario autenticado.
     *
     * <p>Valide el access token y consulta el estado actual del usuario,
     * sus roles y permisos.</p>
     *
     * @param authentication Contexto de autenticación del usuario
     * @return UserResponse con datos y permisos actuales
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        String userId = authentication.getName();
        UserResponse userResponse = authService.getMe(userId);

        return ResponseEntity.ok(ApiResponse.ok("Sesión recuperada", userResponse));
    }

    /**
     * Actualiza el perfil del usuario autenticado.
     *
     * <p>El usuario puede cambiar su nombre, email y foto de perfil.
     * La foto es opcional — si se provee, se sube internamente.</p>
     *
     * @param name Nombre a actualizar (opcional)
     * @param email Email a actualizar (opcional)
     * @param photo Foto de perfil (opcional, máximo 2MB)
     * @param authentication Contexto de autenticación del usuario
     * @return Datos actualizados
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile photo,
            Authentication authentication) {

        String userId = authentication.getName();
        UserResponse userResponse = authService.updateMe(userId, name, photo);

        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado", userResponse));
    }

    /**
     * Solicita un código OTP de recuperación de contraseña.
     *
     * <p>La respuesta es opaca: no revela si el correo está registrado.</p>
     *
     * @param request Email del usuario
     * @return Mensaje opaco
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        authService.forgotPassword(request, httpRequest);

        return ResponseEntity.ok(ApiResponse.ok(
                "Si el correo está registrado, recibirás un código de recuperación."
        ));
    }

    /**
     * Verifica el OTP y devuelve un token de recuperación.
     *
     * @param request Email y OTP
     * @return OtpTokenResponse con el token de recuperación
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<OtpTokenResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        OtpTokenResponse tokenResponse = authService.verifyOtp(request, httpRequest);

        return ResponseEntity.ok(ApiResponse.ok("OTP verificado", tokenResponse));
    }

    /**
     * Restablece la contraseña con un token de recuperación.
     *
     * @param request Token y nueva contraseña
     * @return Mensaje de confirmación
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        authService.resetPassword(request, httpRequest);

        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada correctamente"));
    }

    /**
     * Verifica el correo electrónico con un token.
     *
     * @param request Token de verificación
     * @return Mensaje de confirmación
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        authService.verifyEmail(request);

        return ResponseEntity.ok(ApiResponse.ok("Correo verificado correctamente"));
    }

    /**
     * Reenvía el correo de verificación.
     *
     * @param request Email del usuario
     * @return Mensaje opaco
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.resendVerification(request.getEmail());

        return ResponseEntity.ok(ApiResponse.ok(
                "Si la dirección puede utilizarse, recibirás instrucciones para verificar la cuenta."
        ));
    }
}
