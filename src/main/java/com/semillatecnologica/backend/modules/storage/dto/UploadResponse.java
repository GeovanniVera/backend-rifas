package com.semillatecnologica.backend.modules.storage.dto;

import com.semillatecnologica.backend.modules.storage.model.StoredFileEntity;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para upload de archivos.
 */
public record UploadResponse(
    String id,
    String originalName,
    String mimeType,
    long sizeBytes,
    String storageTarget,
    String url,
    LocalDateTime createdAt
) {
    public static UploadResponse from(StoredFileEntity entity) {
        return new UploadResponse(
                entity.getId(),
                entity.getOriginalName(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getStorageTarget(),
                entity.getPublicUrl(),
                entity.getCreatedAt()
        );
    }
}
