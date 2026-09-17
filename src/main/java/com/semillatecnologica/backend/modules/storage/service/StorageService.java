package com.semillatecnologica.backend.modules.storage.service;

import com.semillatecnologica.backend.modules.storage.dto.FileMetadataResponse;
import com.semillatecnologica.backend.modules.storage.dto.UploadResponse;
import com.semillatecnologica.backend.modules.storage.factory.StorageStrategyFactory;
import com.semillatecnologica.backend.modules.storage.model.StoredFileEntity;
import com.semillatecnologica.backend.modules.storage.repository.StoredFileRepository;
import com.semillatecnologica.backend.modules.storage.strategy.IStorageStrategy;
import com.semillatecnologica.backend.modules.storage.strategy.StoredFile;
import com.semillatecnologica.backend.modules.storage.validator.FileSecurityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

/**
 * Servicio orquestador de almacenamiento.
 *
 * <p>Flujo:
 * <pre>
 * StorageService.upload(file, userId, ...)
 *   → FileSecurityValidator.validate(file)
 *   → StorageStrategyFactory.resolve()
 *   → IStorageStrategy.upload(file, storageKey)
 *   → guardar metadata en BD
 *   → StoredFile
 * </pre>
 *
 * <p>Aplica las reglas comunes antes de delegar a la estrategia:
 * validación de seguridad, nombre generado por el servidor,
 * y persistencia de metadata.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final FileSecurityValidator validator;
    private final StorageStrategyFactory strategyFactory;
    private final StoredFileRepository storedFileRepository;

    /**
     * Sube un archivo al almacenamiento configurado.
     *
     * @param file Archivo multipart
     * @param userId ID del usuario que sube el archivo
     * @param entityType Tipo de entidad vinculada (nullable)
     * @param entityId ID de la entidad vinculada (nullable)
     * @return respuesta con metadata del archivo
     */
    @Transactional
    public UploadResponse upload(MultipartFile file, String userId,
                                  String entityType, String entityId) {
        // 1. Validar seguridad
        validator.validate(file);

        // 2. Generar storage key única
        String storageKey = generateStorageKey(file.getOriginalFilename());

        // 3. Resolver estrategia
        IStorageStrategy strategy = strategyFactory.resolve()
                .orElseThrow(() -> new RuntimeException(
                        "No hay estrategia de almacenamiento configurada"));

        // 4. Subir
        StoredFile stored = strategy.upload(file, storageKey);

        // 5. Persistir metadata en BD
        StoredFileEntity entity = StoredFileEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .originalName(file.getOriginalFilename())
                .storageKey(stored.getStorageKey())
                .mimeType(stored.getMimeType())
                .sizeBytes(stored.getSizeBytes())
                .storageTarget(stored.getTarget())
                .publicUrl(stored.getAccessUrl())
                .entityType(entityType)
                .entityId(entityId)
                .build();

        storedFileRepository.save(entity);

        log.info("Archivo subido: userId={} key={} size={}",
                userId, storageKey, file.getSize());

        return UploadResponse.from(entity);
    }

    /**
     * Elimina un archivo por su ID.
     *
     * @param fileId ID del archivo en BD
     * @param userId ID del usuario (verificación de propiedad)
     * @return true si se eliminó
     */
    @Transactional
    public boolean delete(String fileId, String userId) {
        Optional<StoredFileEntity> opt = storedFileRepository.findById(fileId);
        if (opt.isEmpty()) return false;

        StoredFileEntity entity = opt.get();
        if (!entity.getUserId().equals(userId)) return false;

        // Eliminar de la estrategia
        strategyFactory.resolve().ifPresent(strategy ->
                strategy.delete(entity.getStorageKey())
        );

        // Eliminar de BD
        storedFileRepository.delete(entity);

        log.info("Archivo eliminado: fileId={} key={}", fileId, entity.getStorageKey());
        return true;
    }

    /**
     * Retorna la metadata de un archivo.
     *
     * @param fileId ID del archivo
     * @return Optional con la metadata
     */
    @Transactional(readOnly = true)
    public Optional<FileMetadataResponse> getMetadata(String fileId) {
        return storedFileRepository.findById(fileId)
                .map(FileMetadataResponse::from);
    }

    /**
     * Retorna una URL de acceso al archivo.
     *
     * @param fileId ID del archivo
     * @param expiresInMinutes Minutos de expiración (0 = permanente)
     * @return URL de acceso
     */
    @Transactional(readOnly = true)
    public Optional<String> getAccessUrl(String fileId, int expiresInMinutes) {
        return storedFileRepository.findById(fileId).flatMap(entity ->
                strategyFactory.resolve().map(strategy ->
                        strategy.getAccessUrl(entity.getStorageKey(), expiresInMinutes)
                )
        );
    }

    /**
     * Genera una storage key única basada en timestamp + UUID corto + extensión.
     */
    private String generateStorageKey(String originalName) {
        String ext = FileSecurityValidator.getExtension(originalName);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long timestamp = System.currentTimeMillis();
        return timestamp + "-" + uuid + (ext.isEmpty() ? "" : "." + ext);
    }
}
