package com.semillatecnologica.backend.modules.storage.factory;

import com.semillatecnologica.backend.modules.storage.strategy.CloudinaryStorage;
import com.semillatecnologica.backend.modules.storage.strategy.IStorageStrategy;
import com.semillatecnologica.backend.modules.storage.strategy.LocalDiskStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Fábrica que resuelve la estrategia de almacenamiento según configuración.
 *
 * <p>Selecta entre Local y Cloudinary según la propiedad
 * {@code storage.target} en application.yaml.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorageStrategyFactory {

    private final LocalDiskStorage localDiskStorage;
    private final CloudinaryStorage cloudinaryStorage;

    @Value("${storage.target:LOCAL}")
    private String configuredTarget;

    private Map<String, IStorageStrategy> strategies;

    @jakarta.annotation.PostConstruct
    public void init() {
        strategies = Map.of(
                "LOCAL", localDiskStorage,
                "CLOUDINARY", cloudinaryStorage
        );
        log.info("Estrategias de almacenamiento registradas: {}", strategies.keySet());
    }

    /**
     * Resuelve la estrategia configurada para el entorno actual.
     *
     * @return Optional con la estrategia si existe
     */
    public Optional<IStorageStrategy> resolve() {
        return resolve(configuredTarget);
    }

    /**
     * Resuelve una estrategia por nombre.
     *
     * @param target Nombre de la estrategia (LOCAL, CLOUDINARY)
     * @return Optional con la estrategia si existe
     */
    public Optional<IStorageStrategy> resolve(String target) {
        IStorageStrategy strategy = strategies.get(target.toUpperCase());
        if (strategy == null) {
            log.warn("Estrategia de almacenamiento no encontrada: {}", target);
            return Optional.empty();
        }

        // Cloudinary verifica si está configurado
        if (strategy instanceof CloudinaryStorage cloudinary && !cloudinary.isConfigured()) {
            log.warn("Cloudinary no está configurado, no se puede usar");
            return Optional.empty();
        }

        return Optional.of(strategy);
    }

    /**
     * Retorna la estrategia configurada actualmente.
     */
    public String getConfiguredTarget() {
        return configuredTarget;
    }
}
