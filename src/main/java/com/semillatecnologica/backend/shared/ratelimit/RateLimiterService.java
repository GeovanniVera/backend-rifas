package com.semillatecnologica.backend.shared.ratelimit;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de rate limiting en memoria.
 *
 * <p>Implementa ventanas fijas por clave. Cada clave representa
 * un bucket (login:email:x, login:ip:y, etc.).</p>
 *
 * <p>En memoria: suficiente para una instancia. Para múltiples
 * instancias o producción se migrará a Redis (v2).</p>
 */
@Service
public class RateLimiterService {

    private record Entry(int count, long windowStartMillis) {}

    private final Map<String, Entry> buckets = new ConcurrentHashMap<>();

    /**
     * Intenta consumir una unidad del bucket.
     *
     * @param key Clave del bucket (ej: "login:email:user@x.com")
     * @param maxAttempts Máximo de intentos permitidos en la ventana
     * @param windowSeconds Duración de la ventana en segundos
     * @return true si se permite la acción, false si excede el límite
     */
    public boolean tryConsume(String key, int maxAttempts, long windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000;

        Entry current = buckets.compute(key, (k, entry) -> {
            if (entry == null || now - entry.windowStartMillis() >= windowMillis) {
                return new Entry(1, now);
            }
            return new Entry(entry.count() + 1, entry.windowStartMillis());
        });

        return current.count() <= maxAttempts;
    }

    /**
     * Registra un intento fallido y retorna si se permite el siguiente.
     */
    public boolean recordFailure(String key, int maxAttempts, long windowSeconds) {
        return tryConsume(key, maxAttempts, windowSeconds);
    }

    /**
     * Resetea un bucket (ej: login exitoso).
     */
    public void reset(String key) {
        buckets.remove(key);
    }

    /**
     * Limpia buckets expirados para evitar crecimiento infinito.
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        // Los buckets se limpian solos al expirar en tryConsume;
        // aquí solo removemos los que ya pasaron su ventana máxima.
        // Como no guardamos la duración por bucket, esto es un respaldo:
        // removemos entradas con más de 1 hora de antigüedad.
        buckets.entrySet().removeIf(e ->
            now - e.getValue().windowStartMillis() > 3600_000);
    }
}