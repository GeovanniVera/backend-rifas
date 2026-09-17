package com.semillatecnologica.backend.modules.notification.channel;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Implementación del canal de email.
 *
 * <p>Utiliza Spring Mail para enviar emails.
 * En desarrollo, los emails se capturan en MailHog.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailChannel implements INotificationChannel {

    private final JavaMailSender mailSender;

    @Value("${notifications.email.from:noreply@tiendarifas.com}")
    private String fromEmail;

    @Value("${notifications.email.enabled:true}")
    private boolean enabled;

    @Override
    public String getChannelType() {
        return "EMAIL";
    }

    @Override
    public DeliveryResult send(String recipient, String subject, String body, NotificationContext context) {
        if (!isEnabled()) {
            log.debug("Canal EMAIL deshabilitado, omitiendo envío a {}", recipient);
            return DeliveryResult.failure(getChannelType(), recipient, "Canal deshabilitado");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);

            log.info("Email enviado a {}: {}", recipient, subject);
            return DeliveryResult.success(getChannelType(), recipient, null);

        } catch (MessagingException e) {
            log.error("Error al enviar email a {}: {}", recipient, e.getMessage(), e);
            return DeliveryResult.failure(getChannelType(), recipient, e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
