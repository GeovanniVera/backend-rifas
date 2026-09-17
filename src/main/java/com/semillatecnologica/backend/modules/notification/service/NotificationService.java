package com.semillatecnologica.backend.modules.notification.service;

import com.semillatecnologica.backend.modules.notification.channel.*;
import com.semillatecnologica.backend.modules.notification.inapp.model.InAppNotification;
import com.semillatecnologica.backend.modules.notification.inapp.service.InAppNotificationService;
import com.semillatecnologica.backend.modules.notification.template.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Servicio orquestador de notificaciones.
 *
 * <p>Es el único punto de entrada del módulo. Recibe una notificación
 * de dominio, renderiza la plantilla, selecciona el canal mediante
 * la fábrica y delega el envío.</p>
 *
 * <p>Flujo:
 * <pre>
 * NotificationService.notify(notification)
 *   → TemplateRenderer.render(...)
 *   → NotificationChannelFactory.resolve(channel)
 *   → INotificationChannel.send(...)
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationChannelFactory channelFactory;
    private final TemplateRenderer templateRenderer;
    private final InAppNotificationService inAppNotificationService;

    @Value("${notifications.email.verification.base-url}")
    private String verificationBaseUrl;

    @Value("${notifications.email.password-reset.base-url}")
    private String passwordResetBaseUrl;

    /**
     * Envía una notificación por el canal especificado.
     *
     * @param channelType Tipo de canal (EMAIL, SMS)
     * @param recipient Destinatario
     * @param subject Asunto
     * @param templateName Nombre de la plantilla
     * @param templateVariables Variables para la plantilla
     * @return resultado del envío
     */
    public DeliveryResult notify(String channelType, String recipient, String subject,
                                 String templateName, Map<String, Object> templateVariables) {
        INotificationChannel channel = channelFactory.resolveEnabled(channelType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Canal no disponible: " + channelType));

        String body = templateRenderer.render(templateName, templateVariables);
        NotificationContext context = NotificationContext.withTemplate(templateName, templateVariables);

        return channel.send(recipient, subject, body, context);
    }

    /**
     * Envía email de verificación de cuenta.
     */
    @Async
    public void sendVerificationEmail(String to, String userName, String verificationToken) {
        String verificationUrl = verificationBaseUrl + "?token=" + verificationToken;
        String subject = "Verifica tu cuenta - TiendaRifas";

        Map<String, Object> variables = Map.of(
                "userName", userName,
                "verificationUrl", verificationUrl
        );

        notify("EMAIL", to, subject, "verification", variables);
    }

    /**
     * Envía email de bienvenida.
     */
    @Async
    public void sendWelcomeEmail(String to, String userName) {
        String subject = "¡Bienvenido a TiendaRifas!";

        Map<String, Object> variables = Map.of(
                "userName", userName
        );

        notify("EMAIL", to, subject, "welcome", variables);
    }

    /**
     * Envía email de restablecimiento de contraseña.
     */
    @Async
    public void sendPasswordResetEmail(String to, String userName, String resetToken) {
        String resetUrl = passwordResetBaseUrl + "?token=" + resetToken;
        String subject = "Restablece tu contraseña - TiendaRifas";

        Map<String, Object> variables = Map.of(
                "userName", userName,
                "resetUrl", resetUrl
        );

        notify("EMAIL", to, subject, "password-reset", variables);
    }

    /**
     * Envía email con OTP de recuperación de contraseña.
     */
    @Async
    public void sendPasswordResetOtp(String to, String userName, String otp) {
        String subject = "Tu código de recuperación - TiendaRifas";

        Map<String, Object> variables = Map.of(
                "userName", userName,
                "otp", otp
        );

        notify("EMAIL", to, subject, "password-reset-otp", variables);
    }

    // =====================================================================
    // Notificaciones in-app
    // =====================================================================

    /**
     * Crea una notificación in-app para un usuario.
     *
     * <p>Punto de entrada unificado para crear notificaciones persistidas
     * que el frontend muestra en la UI.</p>
     *
     * @param userId ID del usuario destinatario
     * @param type Tipo: SECURITY, WORKFLOW, SYSTEM, SOCIAL
     * @param title Título corto
     * @param body Cuerpo detallado
     * @return la notificación creada
     */
    public InAppNotification createInAppNotification(String userId, String type,
                                                      String title, String body) {
        return inAppNotificationService.create(userId, type, title, body, null, null, null);
    }

    /**
     * Crea una notificación in-app con vinculación a una entidad.
     *
     * @param userId ID del usuario destinatario
     * @param type Tipo: SECURITY, WORKFLOW, SYSTEM, SOCIAL
     * @param title Título corto
     * @param body Cuerpo detallado
     * @param entityType Tipo de entidad relacionada
     * @param entityId ID de la entidad relacionada
     * @param actionUrl URL de deep-link
     * @return la notificación creada
     */
    public InAppNotification createInAppNotification(String userId, String type,
                                                      String title, String body,
                                                      String entityType, String entityId,
                                                      String actionUrl) {
        return inAppNotificationService.create(userId, type, title, body,
                entityType, entityId, actionUrl);
    }
}
