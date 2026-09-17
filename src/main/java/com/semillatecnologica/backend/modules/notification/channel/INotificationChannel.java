package com.semillatecnologica.backend.modules.notification.channel;

/**
 * Interfaz Strategy para canales de notificación.
 *
 * <p>Cada implementación encapsula un canal y su proveedor.
 * Cambiar de proveedor (SendGrid → SES) no modifica el contrato.</p>
 */
public interface INotificationChannel {

    /**
     * Identificador único del canal (EMAIL, SMS, WHATSAPP).
     */
    String getChannelType();

    /**
     * Envía una notificación por el canal específico.
     *
     * @param recipient Destinatario (email, teléfono, device token)
     * @param subject Asunto/título de la notificación
     * @param body Cuerpo del mensaje (HTML o texto plano)
     * @param context Variables adicionales para el template
     * @return resultado del envío
     */
    DeliveryResult send(String recipient, String subject, String body, NotificationContext context);

    /**
     * Indica si este canal está habilitado en la configuración actual.
     */
    boolean isEnabled();
}
