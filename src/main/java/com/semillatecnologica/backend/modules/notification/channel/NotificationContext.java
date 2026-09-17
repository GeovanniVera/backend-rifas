package com.semillatecnologica.backend.modules.notification.channel;

import java.util.Map;

/**
 * Contexto adicional para el envío de notificaciones.
 *
 * <p>Contiene variables que los canales pueden usar
 * para personalizar el envío (template variables, metadata, etc.)</p>
 */
public record NotificationContext(
    Map<String, Object> templateVariables,
    String templateName,
    String priority,
    String correlationId
) {
    public static NotificationContext empty() {
        return new NotificationContext(Map.of(), null, "normal", null);
    }

    public static NotificationContext withTemplate(String templateName, Map<String, Object> variables) {
        return new NotificationContext(variables, templateName, "normal", null);
    }
}
