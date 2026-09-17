package com.semillatecnologica.backend.modules.notification.channel;

/**
 * Resultado del envío de una notificación.
 */
public record DeliveryResult(
    boolean success,
    String channelType,
    String recipient,
    String errorMessage,
    String providerMessageId
) {
    public static DeliveryResult success(String channelType, String recipient, String providerMessageId) {
        return new DeliveryResult(true, channelType, recipient, null, providerMessageId);
    }

    public static DeliveryResult failure(String channelType, String recipient, String errorMessage) {
        return new DeliveryResult(false, channelType, recipient, errorMessage, null);
    }
}
