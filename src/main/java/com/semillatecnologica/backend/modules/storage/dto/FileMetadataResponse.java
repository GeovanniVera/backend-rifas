package com.semillatecnologica.backend.modules.storage.dto;

import com.semillatecnologica.backend.modules.storage.model.StoredFileEntity;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con metadata completa de un archivo.
 */
public record FileMetadataResponse(
    String id,
    String originalName,
    String mimeType,
    long sizeBytes,
    String storageTarget,
    String url,
    String entityType,
    String entityId,
    LocalDateTime createdAt
) {
    public static FileMetadataResponse from(StoredFileEntity entity) {
        return new FileMetadataResponse(
                entity.getId(),
                entity.getOriginalName(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getStorageTarget(),
                entity.getPublicUrl(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getCreatedAt()
        );
    }
}
