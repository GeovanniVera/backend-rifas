package com.semillatecnologica.backend.modules.auth.scheduler;

import com.semillatecnologica.backend.modules.auth.repository.OtpChallengeRepository;
import com.semillatecnologica.backend.modules.auth.repository.RefreshTokenRepository;
import com.semillatecnologica.backend.modules.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduler de limpieza de tokens expirados.
 *
 * <p>Ejecuta limpieza batch una vez al día para eliminar tokens,
 * desafíos OTP y tokens de verificación que ya expiraron.</p>
 *
 * <p>Estrategia híbrida:
 * <ul>
 *   <li>Lazy: al validar un token expirado, se borra en ese momento</li>
 *   <li>Scheduled: limpieza batch para tokens huérfanos nunca accedidos</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final OtpChallengeRepository otpChallengeRepository;

    /**
     * Limpia tokens expirados todos los días a las 3:00 AM.
     *
     * <p>Cron: segundo minuto hora día mes día-semana
     * "0 0 3 * * *" = todos los días a las 3:00 AM</p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Iniciando limpieza de tokens expirados...");

        try {
            int deletedRefresh = refreshTokenRepository.deleteAllExpired(now);
            log.info("Refresh tokens eliminados: {}", deletedRefresh);
        } catch (Exception e) {
            log.error("Error al limpiar refresh tokens: {}", e.getMessage());
        }

        try {
            int deletedVerification = verificationTokenRepository.deleteAllExpired(now);
            log.info("Verification tokens eliminados: {}", deletedVerification);
        } catch (Exception e) {
            log.error("Error al limpiar verification tokens: {}", e.getMessage());
        }

        try {
            int deletedOtp = otpChallengeRepository.deleteAllExpired(now);
            log.info("OTP challenges eliminados: {}", deletedOtp);
        } catch (Exception e) {
            log.error("Error al limpiar OTP challenges: {}", e.getMessage());
        }

        log.info("Limpieza de tokens completada");
    }
}
