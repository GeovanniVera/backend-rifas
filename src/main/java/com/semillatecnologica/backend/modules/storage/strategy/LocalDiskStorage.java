package com.semillatecnologica.backend.modules.storage.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Estrategia de almacenamiento en disco local.
 *
 * <p>Ideal para desarrollo y entornos controlados.
 * Los archivos se almacenan en un directorio configurable.</p>
 *
 * <p>La ruta base se resuelve a absoluta y normalizada para evitar
 * depender del working directory del proceso (que puede ser el
 * directorio temporal de Tomcat en Spring Boot embebido).</p>
 *
 * <p>Las URLs de acceso son relativas al servidor; no generan
 * URLs firmadas (el acceso es directo vía controller).</p>
 */
@Component
@Slf4j
public class LocalDiskStorage implements IStorageStrategy {

    private static final String TARGET = "LOCAL";

    @Value("${storage.local.base-path:${user.dir}/uploads}")
    private String basePath;

    @Value("${storage.local.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    /**
     * Resuelve la ruta base a absoluta y normalizada.
     */
    private Path resolveBasePath() {
        return Paths.get(basePath).toAbsolutePath().normalize();
    }

    /**
     * Genera la URL pública absoluta del archivo,
     * consistente con la que devuelve Cloudinary.
     */
    private String buildPublicUrl(String storageKey) {
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return base + "/api/files/" + storageKey;
    }

    @Override
    public StoredFile upload(MultipartFile file, String storageKey) {
        try {
            Path targetDir = resolveBasePath();
            Files.createDirectories(targetDir);

            Path filePath = targetDir.resolve(storageKey);
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath.toFile());

            log.info("Archivo almacenado en disco: {}", filePath);

            return StoredFile.builder()
                    .storageKey(storageKey)
                    .originalName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .accessUrl(buildPublicUrl(storageKey))
                    .target(TARGET)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Error al almacenar archivo en disco: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path filePath = resolveBasePath().resolve(storageKey);
            Files.deleteIfExists(filePath);
            log.info("Archivo eliminado del disco: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar archivo del disco: " + e.getMessage(), e);
        }
    }

    @Override
    public String getAccessUrl(String storageKey, int expiresInMinutes) {
        // Disco local: URL absoluta sin expiración
        return buildPublicUrl(storageKey);
    }

    @Override
    public String getTarget() {
        return TARGET;
    }
}