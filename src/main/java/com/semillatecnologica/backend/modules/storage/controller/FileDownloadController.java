package com.semillatecnologica.backend.modules.storage.controller;

import com.semillatecnologica.backend.modules.storage.strategy.LocalDiskStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controlador para servir archivos almacenados localmente.
 *
 * <p>Únicamente expone el binario para lectura (fotos de perfil, etc.).
 * No permite subir, eliminar ni gestionar archivos — eso es interno.</p>
 *
 * <p>Los storage keys son opacos (UUID + timestamp), no enumerables.
 * En producción con Cloudinary se usarán URLs firmadas en su lugar.</p>
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Archivos (lectura)", description = "Sirve binarios almacenados localmente")
public class FileDownloadController {

    private final LocalDiskStorage localDiskStorage;

    @Value("${storage.local.base-path:${user.dir}/uploads}")
    private String basePath;

    /**
     * Descarga un archivo por su storage key.
     *
     * @param storageKey Clave del archivo (timestamp-uuid.ext)
     * @return Binario del archivo
     */
    @GetMapping("/{storageKey}")
    @Operation(summary = "Descargar archivo",
               description = "Sirve el binario de un archivo almacenado localmente.")
    public ResponseEntity<Resource> download(@PathVariable String storageKey) {
        try {
            Path baseDir = Paths.get(basePath).toAbsolutePath().normalize();
            Path filePath = baseDir.resolve(storageKey).normalize();

            // Prevenir path traversal: el archivo debe quedar dentro del directorio base
            if (!filePath.startsWith(baseDir)) {
                return ResponseEntity.badRequest().build();
            }

            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error al servir archivo {}: {}", storageKey, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}