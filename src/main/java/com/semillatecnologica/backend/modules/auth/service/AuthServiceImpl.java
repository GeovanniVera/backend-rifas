package com.semillatecnologica.backend.modules.auth.service;

import com.semillatecnologica.backend.modules.admin.service.AuditService;
import com.semillatecnologica.backend.modules.auth.dto.*;
import com.semillatecnologica.backend.modules.auth.model.*;
import com.semillatecnologica.backend.modules.auth.repository.*;
import com.semillatecnologica.backend.modules.notification.service.NotificationService;
import com.semillatecnologica.backend.modules.storage.service.StorageService;
import com.semillatecnologica.backend.security.cookie.AuthCookieService;
import com.semillatecnologica.backend.security.jwt.JwtTokenService;
import com.semillatecnologica.backend.shared.exception.*;
import com.semillatecnologica.backend.shared.ratelimit.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación del servicio de autenticación.
 *
 * <p>Orquesta login, registro, refresh, logout y recuperación de contraseña.
 * Utiliza argon2 para hash de contraseñas, JWT para access tokens y
 * refresh tokens opacos en cookies HttpOnly.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final OtpChallengeRepository otpChallengeRepository;
    private final JwtTokenService jwtTokenService;
    private final AuthCookieService authCookieService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final StorageService storageService;
    private final RateLimiterService rateLimiterService;

    @Value("${auth.jwt.access-token-expiration:900}")
    private int accessTokenExpiration;

    // ===== Rate limiting (OWASP A07) =====
    private static final int LOGIN_MAX_ACCOUNT = 5;
    private static final int LOGIN_MAX_IP = 20;
    private static final int LOGIN_MAX_COMBINED = 5;
    private static final long LOGIN_WINDOW_SECONDS = 15 * 60; // 15 min

    private static final int OTP_MAX_ACCOUNT = 3;
    private static final int OTP_MAX_IP = 10;
    private static final long OTP_WINDOW_SECONDS = 3600; // 1 hora

    private static final int OTP_VERIFY_MAX_ACCOUNT = 5;
    private static final long OTP_VERIFY_WINDOW_SECONDS = 15 * 60;

    private static final int REGISTER_MAX_IP = 3;
    private static final long REGISTER_WINDOW_SECONDS = 3600; // 1 hora

    /**
     * Verifica el rate limit de login (capa triple: cuenta, IP, cuenta+IP).
     */
    private void checkLoginRateLimit(String email, String ip) {
        String keyAccount = "login:account:" + email;
        String keyIp = "login:ip:" + ip;
        String keyCombined = "login:combined:" + email + ":" + ip;

        if (!rateLimiterService.tryConsume(keyCombined, LOGIN_MAX_COMBINED, LOGIN_WINDOW_SECONDS)
                || !rateLimiterService.tryConsume(keyAccount, LOGIN_MAX_ACCOUNT, LOGIN_WINDOW_SECONDS)
                || !rateLimiterService.tryConsume(keyIp, LOGIN_MAX_IP, LOGIN_WINDOW_SECONDS)) {
            throw new RateLimitExceededException(
                    "Demasiados intentos de inicio de sesión. Intentá de nuevo más tarde.");
        }
    }

    /**
     * Resetea los buckets de login tras un intento exitoso.
     */
    private void resetLoginRateLimit(String email, String ip) {
        rateLimiterService.reset("login:account:" + email);
        rateLimiterService.reset("login:ip:" + ip);
        rateLimiterService.reset("login:combined:" + email + ":" + ip);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = normalizeEmail(request.getEmail());
        String ip = getClientIp(httpRequest);

        // Rate limit: capa triple (cuenta, IP, cuenta+IP)
        checkLoginRateLimit(email, ip);

        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> {
                    auditService.log("LOGIN_FAILED", "USER", null, null,
                            Map.of("email", email), null, httpRequest);
                    return new UnauthorizedException("Credenciales incorrectas");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditService.log("LOGIN_FAILED", "USER", user.getId(), user.getId(),
                    null, null, httpRequest);
            throw new UnauthorizedException("Credenciales incorrectas");
        }

        if (!user.getIsVerified()) {
            throw new ForbiddenException("Cuenta no verificada");
        }

        if (user.getSuspended() != null && user.getSuspended()) {
            throw new ForbiddenException("Cuenta suspendida");
        }

        TokenResponse response = createSession(user, httpRequest, httpResponse);

        // Login exitoso: resetear rate limit
        resetLoginRateLimit(email, ip);

        auditService.log("LOGIN_SUCCEEDED", "USER", user.getId(), user.getId(),
                null, Map.of("email", user.getEmail()), httpRequest);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());

        // Rate limit: máximo 3 registros por IP por hora
        if (!rateLimiterService.tryConsume("register:ip:" + getClientIp(httpRequest),
                REGISTER_MAX_IP, REGISTER_WINDOW_SECONDS)) {
            throw new RateLimitExceededException(
                    "Demasiados registros desde esta dirección. Intentá de nuevo más tarde.");
        }

        if (userRepository.existsByEmail(email)) {
            // Respuesta opaca: no revelar que el correo ya existe
            log.debug("Registro intentado con correo existente: {}", email);
            return;
        }

        Role defaultRole = roleRepository.findByName("viewer")
                .orElseThrow(() -> new RoleNotFoundException("viewer"));

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .name(sanitizeName(request.getName()))
                .password(passwordEncoder.encode(request.getPassword()))
                .isVerified(false)
                .termsAcceptedAt(LocalDateTime.now())
                .termsVersion("1.0")
                .build();

        user.addRole(defaultRole);
        userRepository.save(user);

        // Crear token de verificación
        String verificationToken = UUID.randomUUID().toString();
        VerificationToken token = VerificationToken.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .tokenHash(hashToken(verificationToken))
                .purpose("email_verification")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        verificationTokenRepository.save(token);

        // Enviar email de verificación
        notificationService.sendVerificationEmail(email, request.getName(), verificationToken);

        log.info("Usuario registrado: {}", email);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TokenResponse refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String tokenValue = authCookieService.getRefreshTokenFromCookie(httpRequest);
        if (tokenValue == null) {
            throw new UnauthorizedException("Refresh token no encontrado");
        }

        String tokenHash = hashToken(tokenValue);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            // Lazy cleanup: borrar token expirado
            if (refreshToken.isExpired()) {
                refreshTokenRepository.delete(refreshToken);
            }

            // Posible reutilización: revocar toda la familia
            if (refreshToken.isRevoked()) {
                log.warn("Reutilización de refresh token detectada, revocando familia: {}",
                        refreshToken.getFamilyId());
                refreshTokenRepository.revokeAllByUserId(
                        refreshToken.getUser().getId(),
                        LocalDateTime.now()
                );
            }
            throw new UnauthorizedException("Refresh token inválido o expirado");
        }

        if (refreshToken.isUsed()) {
            // Token ya utilizado: posible reutilización
            log.warn("Refresh token ya utilizado, revocando familia: {}",
                    refreshToken.getFamilyId());
            refreshTokenRepository.revokeAllByUserId(
                    refreshToken.getUser().getId(),
                    LocalDateTime.now()
            );
            throw new UnauthorizedException("Refresh token ya utilizado");
        }

        User user = refreshToken.getUser();

        // Marcar el token actual como utilizado
        refreshToken.markAsUsed();

        // Crear nuevo refresh token
        RefreshToken newRefreshToken = createRefreshToken(user, httpRequest);
        refreshToken.setReplacedBy(newRefreshToken.getId());
        refreshTokenRepository.save(refreshToken);

        // Emitir nuevo access token
        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail());

        // Rotar cookie
        authCookieService.addRefreshTokenCookie(httpResponse, newRefreshToken.getTokenHash());

        return TokenResponse.builder()
                .user(UserResponse.fromEntity(user, resolvePhotoUrl(user)))
                .accessToken(accessToken)
                .expiresIn(accessTokenExpiration)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String tokenValue = authCookieService.getRefreshTokenFromCookie(httpRequest);
        if (tokenValue != null) {
            String tokenHash = hashToken(tokenValue);
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(token -> {
                        auditService.log("LOGOUT", "USER", token.getUser().getId(),
                                token.getUser().getId(), null, null, httpRequest);
                        token.revoke();
                        refreshTokenRepository.save(token);
                    });
        }

        authCookieService.removeRefreshTokenCookie(httpResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void logoutAll(String userId) {
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
        auditService.log("LOGOUT", "USER", userId, userId, null,
                Map.of("scope", "all_sessions"), null);
        log.info("Todas las sesiones revocadas para usuario: {}", userId);
    }

    /**
     * Resuelve la URL de la foto de perfil del usuario si existe.
     */
    private String resolvePhotoUrl(User user) {
        if (user.getPhotoFileId() == null) return null;
        return storageService.getAccessUrl(user.getPhotoFileId(), 0).orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe(String userId) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new NotFoundException("Usuario"));

        return UserResponse.fromEntity(user, resolvePhotoUrl(user));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse updateMe(String userId, String name,
                                  org.springframework.web.multipart.MultipartFile photo) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new NotFoundException("Usuario"));

        if (name != null) {
            user.setName(sanitizeName(name));
        }

        // Nota: el cambio de email NO está habilitado.
        // Si se requiere en el futuro, implementar con email pendiente
        // (el activo sigue funcionando hasta verificar el nuevo).

        // Subir foto internamente si se proporciona
        if (photo != null && !photo.isEmpty()) {
            var uploadResponse = storageService.upload(photo, userId, "USER", userId);
            user.setPhotoFileId(uploadResponse.id());
        }

        return UserResponse.fromEntity(user, resolvePhotoUrl(user));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());
        String ip = getClientIp(httpRequest);

        // Rate limit: máximo 3 OTP por cuenta por hora, 10 por IP
        if (!rateLimiterService.tryConsume("otp:account:" + email,
                OTP_MAX_ACCOUNT, OTP_WINDOW_SECONDS)
                || !rateLimiterService.tryConsume("otp:ip:" + ip,
                OTP_MAX_IP, OTP_WINDOW_SECONDS)) {
            throw new RateLimitExceededException(
                    "Demasiadas solicitudes de recuperación. Intentá de nuevo más tarde.");
        }

        // Respuesta opaca: no revelar si la cuenta existe
        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalidar OTPs anteriores
            otpChallengeRepository.invalidateAllByUserIdAndPurpose(user.getId(), "password_reset");

            // Generar OTP de 6 dígitos
            String otp = generateOtp();
            String otpHash = hashToken(otp);

            OtpChallenge challenge = OtpChallenge.builder()
                    .id(UUID.randomUUID().toString())
                    .user(user)
                    .otpHash(otpHash)
                    .purpose("password_reset")
                    .maxAttempts(5)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            otpChallengeRepository.save(challenge);

            // Enviar OTP por email
            notificationService.sendPasswordResetOtp(email, user.getName(), otp);

            log.info("OTP enviado por email para usuario: {}", email);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OtpTokenResponse verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());
        String ip = getClientIp(httpRequest);

        // Rate limit: máximo 5 intentos de verificación por cuenta, 20 por IP
        if (!rateLimiterService.tryConsume("otpverify:account:" + email,
                OTP_VERIFY_MAX_ACCOUNT, OTP_VERIFY_WINDOW_SECONDS)
                || !rateLimiterService.tryConsume("otpverify:ip:" + ip,
                LOGIN_MAX_IP, LOGIN_WINDOW_SECONDS)) {
            throw new RateLimitExceededException(
                    "Demasiados intentos de verificación. Intentá de nuevo más tarde.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciales incorrectas"));

        OtpChallenge challenge = otpChallengeRepository
                .findActiveByUserIdAndPurpose(user.getId(), "password_reset")
                .orElseThrow(() -> new UnauthorizedException("OTP inválido o expirado"));

        if (challenge.isExpired()) {
            throw new UnauthorizedException("OTP expirado");
        }

        if (challenge.isMaxAttemptsReached()) {
            throw new UnauthorizedException("Máximo de intentos alcanzado");
        }

        String otpHash = hashToken(request.getOtp());
        if (!challenge.getOtpHash().equals(otpHash)) {
            challenge.incrementAttempts();
            otpChallengeRepository.save(challenge);
            throw new UnauthorizedException("OTP incorrecto");
        }

        // OTP correcto: invalidar el desafío
        challenge.incrementAttempts();
        otpChallengeRepository.save(challenge);

        // Generar token de recuperación opaco
        String resetToken = generateOpaqueToken();
        String resetTokenHash = hashToken(resetToken);

        VerificationToken verificationToken = VerificationToken.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .tokenHash(resetTokenHash)
                .purpose("password_reset")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        verificationTokenRepository.save(verificationToken);

        return OtpTokenResponse.builder()
                .resetToken(resetToken)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String tokenHash = hashToken(request.getToken());

        VerificationToken verificationToken = verificationTokenRepository
                .findValidByTokenHashAndPurpose(tokenHash, "password_reset")
                .orElseThrow(() -> new ValidationException("Token inválido o expirado"));

        User user = verificationToken.getUser();

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Marcar token como consumido
        verificationToken.consume();
        verificationTokenRepository.save(verificationToken);

        // Revocar todas las sesiones del usuario
        refreshTokenRepository.revokeAllByUserId(user.getId(), LocalDateTime.now());

        // Registrar evento de auditoría (sin datos sensibles: nunca se loguea la contraseña)
        auditService.log("PASSWORD_CHANGED", "USER", user.getId(), user.getId(),
                null, null, httpRequest);

        // TODO: Enviar notificación de cambio de contraseña
        log.info("Contraseña cambiada para usuario: {}", user.getEmail());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String tokenHash = hashToken(request.getToken());

        VerificationToken verificationToken = verificationTokenRepository
                .findValidByTokenHashAndPurpose(tokenHash, "email_verification")
                .orElseThrow(() -> new ValidationException("Token inválido o expirado"));

        User user = verificationToken.getUser();
        user.setIsVerified(true);
        userRepository.save(user);

        verificationToken.consume();
        verificationTokenRepository.save(verificationToken);

        log.info("Correo verificado para usuario: {}", user.getEmail());
    }

    /**
     * Reenvía el correo de verificación.
     *
     * <p>Respuesta opaca: no revela si el correo está registrado.
     * Si el usuario ya está verificado, no envía nada pero retorna success.</p>
     *
     * @param email Email del usuario
     */
    @Override
    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = normalizeEmail(email);

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            // Si ya está verificado, no hacer nada
            if (user.getIsVerified()) {
                return;
            }

            // Invalidar tokens de verificación anteriores
            verificationTokenRepository.invalidateAllByUserIdAndPurpose(user.getId(), "email_verification");

            // Crear nuevo token de verificación
            String verificationToken = UUID.randomUUID().toString();
            VerificationToken token = VerificationToken.builder()
                    .id(UUID.randomUUID().toString())
                    .user(user)
                    .tokenHash(hashToken(verificationToken))
                    .purpose("email_verification")
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
            verificationTokenRepository.save(token);

            // Enviar email de verificación
            notificationService.sendVerificationEmail(normalizedEmail, user.getName(), verificationToken);

            log.info("Email de verificación reenviado para usuario: {}", normalizedEmail);
        });
    }

    /**
     * Crea una sesión completa: access token + refresh token.
     *
     * @param user Usuario autenticado
     * @param httpRequest Solicitud HTTP
     * @param httpResponse Respuesta HTTP
     * @return TokenResponse con los tokens
     */
    private TokenResponse createSession(User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail());
        RefreshToken refreshToken = createRefreshToken(user, httpRequest);

        authCookieService.addRefreshTokenCookie(httpResponse, refreshToken.getTokenHash());

        return TokenResponse.builder()
                .user(UserResponse.fromEntity(user, resolvePhotoUrl(user)))
                .accessToken(accessToken)
                .expiresIn(accessTokenExpiration)
                .build();
    }

    /**
     * Crea un refresh token y lo persiste en la base de datos.
     *
     * @param user Usuario al que pertenece el token
     * @param httpRequest Solicitud HTTP para IP y User-Agent
     * @return RefreshToken creado (con el valor original en tokenHash)
     */
    private RefreshToken createRefreshToken(User user, HttpServletRequest httpRequest) {
        String tokenValue = generateOpaqueToken();
        String tokenHash = hashToken(tokenValue);

        String familyId = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .familyId(familyId)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Genera un OTP de 6 dígitos.
     *
     * @return Código OTP en texto
     */
    private String generateOtp() {
        int otp = SECURE_RANDOM.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    /**
     * Genera un token opaco aleatorio.
     *
     * @return Token en formato Base64 URL-safe
     */
    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Calcula el hash SHA-256 de un token.
     *
     * @param token Token en texto plano
     * @return Hash hexadecimal del token
     */
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    /**
     * Normaliza el email a minúsculas y sin espacios.
     *
     * @param email Email a normalizar
     * @return Email normalizado
     */
    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    /**
     * Sanitiza el nombre: trim, colapsa espacios múltiples.
     *
     * @param name Nombre a sanitizar
     * @return Nombre sanitizado
     */
    private String sanitizeName(String name) {
        if (name == null) return null;
        return name.trim().replaceAll("\\s+", " ");
    }

    /**
     * Extrae la IP real del cliente, considerando proxies.
     *
     * @param request Solicitud HTTP
     * @return IP del cliente
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
