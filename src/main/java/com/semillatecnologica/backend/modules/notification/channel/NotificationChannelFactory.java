package com.semillatecnologica.backend.modules.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fábrica que resuelve el canal de notificación según el tipo.
 *
 * <p>Implementa el patrón Strategy: registra todas las implementaciones
 * de INotificationChannel y permite resolver por tipo.</p>
 */
@Component
@Slf4j
public class NotificationChannelFactory {

    private final Map<String, INotificationChannel> channels;

    /**
     * Inyecta todas las implementaciones de INotificationChannel
     * y las registra por su tipo.
     */
    public NotificationChannelFactory(List<INotificationChannel> channelList) {
        this.channels = channelList.stream()
                .collect(Collectors.toMap(
                        INotificationChannel::getChannelType,
                        Function.identity()
                ));
        log.info("Canales de notificación registrados: {}", channels.keySet());
    }

    /**
     * Resuelve un canal por su tipo.
     *
     * @param channelType Tipo de canal (EMAIL, SMS, WHATSAPP)
     * @return Optional con el canal si existe
     */
    public Optional<INotificationChannel> resolve(String channelType) {
        return Optional.ofNullable(channels.get(channelType.toUpperCase()));
    }

    /**
     * Resuelve un canal y verifica que esté habilitado.
     *
     * @param channelType Tipo de canal
     * @return Optional con el canal si existe y está habilitado
     */
    public Optional<INotificationChannel> resolveEnabled(String channelType) {
        return resolve(channelType)
                .filter(INotificationChannel::isEnabled);
    }

    /**
     * Retorna todos los canales registrados.
     */
    public Map<String, INotificationChannel> getAllChannels() {
        return Map.copyOf(channels);
    }
}
