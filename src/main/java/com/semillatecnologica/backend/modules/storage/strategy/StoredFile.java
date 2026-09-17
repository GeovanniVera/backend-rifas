package com.semillatecnologica.backend.modules.storage.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de resultado del almacenamiento.
 *
 * <p>Representa la metadata de un archivo almacenado,
 * tanto para estrategia local como Cloudinary.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    private String storageKey;
    private String originalName;
    private String mimeType;
    private long sizeBytes;
    private String accessUrl;
    private String target;
}
