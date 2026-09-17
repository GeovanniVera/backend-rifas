package com.semillatecnologica.backend.modules.storage.strategy;

import com.cloudinary.AuthToken;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Estrategia de almacenamiento en Cloudinary.
 *
 * <p>Utiliza el SDK de Cloudinary para upload, delete y URLs firmadas.
 * Requiere configuración de cloud_name, api_key y api_secret.</p>
 *
 * <p>Cloudinary genera URLs firmadas con expiración para archivos privados,
 * y URLs permanentes para archivos públicos.</p>
 */
@Component
@Slf4j
public class CloudinaryStorage implements IStorageStrategy {

    private static final String TARGET = "CLOUDINARY";

    @Value("${storage.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${storage.cloudinary.api-key:}")
    private String apiKey;

    @Value("${storage.cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${storage.cloudinary.folder:backend}")
    private String folder;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            this.cloudinary = new Cloudinary(Map.of(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret
            ));
            log.info("Cloudinary configurado: cloud_name={}", cloudName);
        } else {
            log.warn("Cloudinary no configurado — storage.cloudinary.* properties faltan");
        }
    }

    @Override
    public StoredFile upload(MultipartFile file, String storageKey) {
        if (!isConfigured() || cloudinary == null) {
            throw new RuntimeException("Cloudinary no está configurado");
        }

        try {
            String publicId = folder + "/" + storageKey;

            Map<String, Object> options = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "auto",
                    "overwrite", true
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(), options);

            String url = (String) result.get("secure_url");

            log.info("Archivo subido a Cloudinary: {}", publicId);

            return StoredFile.builder()
                    .storageKey(storageKey)
                    .originalName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .accessUrl(url)
                    .target(TARGET)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Error al subir a Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (!isConfigured() || cloudinary == null) {
            throw new RuntimeException("Cloudinary no está configurado");
        }

        try {
            String publicId = folder + "/" + storageKey;
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Archivo eliminado de Cloudinary: {}", publicId);
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar de Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public String getAccessUrl(String storageKey, int expiresInMinutes) {
        if (!isConfigured() || cloudinary == null) {
            throw new RuntimeException("Cloudinary no está configurado");
        }

        String publicId = folder + "/" + storageKey;

        if (expiresInMinutes > 0) {
            // URL firmada con expiración usando AuthToken
            long now = System.currentTimeMillis() / 1000;
            long expiration = now + (expiresInMinutes * 60L);

            AuthToken authToken = new AuthToken()
                    .startTime(now)
                    .expiration(expiration)
                    .acl("resource:*");

            return cloudinary.url()
                    .publicId(publicId)
                    .signed(true)
                    .authToken(authToken)
                    .generate();
        }

        // URL permanente
        return cloudinary.url().publicId(publicId).generate();
    }

    @Override
    public String getTarget() {
        return TARGET;
    }

    /**
     * Indica si Cloudinary está configurado y listo para usar.
     */
    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }
}
