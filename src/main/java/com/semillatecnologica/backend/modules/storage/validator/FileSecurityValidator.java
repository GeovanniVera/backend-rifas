package com.semillatecnologica.backend.modules.storage.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Validador de seguridad para archivos.
 *
 * <p>Aplica las reglas antes de permitir un upload:
 * <ul>
 *   <li>Tamaño máximo (configurable, default 2MB)</li>
 *   <li>Extensión en whitelist</li>
 *   <li>MIME type real via stream sniffing (no confiar en el nombre)</li>
 *   <li>Prevención de path traversal</li>
 * </ul>
 */
@Component
@Slf4j
public class FileSecurityValidator {

    /**
     * Whitelist de extensiones permitidas (sin punto).
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // Imágenes
            "jpg", "jpeg", "png", "gif", "webp", "svg",
            // Documentos
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            // Texto
            "txt", "csv", "json", "xml",
            // Otros
            "zip"
    );

    /**
     * Mapeo de magic bytes a MIME types para validación real.
     */
    private static final Map<String, byte[]> MIME_SIGNATURES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png",  new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47},
            "image/gif",  "GIF8".getBytes(),
            "image/webp", "RIFF".getBytes(),
            "application/pdf", "%PDF".getBytes()
    );

    private static final long DEFAULT_MAX_SIZE = 2 * 1024 * 1024; // 2MB

    private final long maxSize;

    public FileSecurityValidator() {
        this.maxSize = DEFAULT_MAX_SIZE;
    }

    public FileSecurityValidator(long maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Valida un archivo contra todas las reglas de seguridad.
     *
     * @param file Archivo a validar
     * @throws SecurityException si no pasa alguna validación
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SecurityException("El archivo está vacío");
        }

        validateSize(file.getSize());
        validateExtension(file.getOriginalFilename());
        validateMimeType(file);
        validatePathTraversal(file.getOriginalFilename());
    }

    /**
     * Valida el tamaño del archivo.
     */
    private void validateSize(long size) {
        if (size > maxSize) {
            throw new SecurityException(
                    "El archivo excede el tamaño máximo de " + (maxSize / 1024 / 1024) + "MB");
        }
    }

    /**
     * Valida que la extensión esté en la whitelist.
     */
    private void validateExtension(String filename) {
        if (filename == null) {
            throw new SecurityException("El archivo no tiene nombre");
        }

        String ext = getExtension(filename).toLowerCase();
        if (ext.isEmpty()) {
            throw new SecurityException("El archivo no tiene extensión");
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new SecurityException(
                    "Extensión no permitida: ." + ext + ". Permitidas: " + ALLOWED_EXTENSIONS);
        }
    }

    /**
     * Valida el MIME type real via stream sniffing.
     * No confía en el Content-Type del cliente.
     */
    private void validateMimeType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                throw new SecurityException("El archivo es demasiado pequeño para validar");
            }

            String detectedMime = detectMime(header);
            String declaredMime = file.getContentType();

            // Si podemos detectar, verificar que coincida con lo declarado
            if (detectedMime != null && declaredMime != null) {
                if (!isCompatibleMime(detectedMime, declaredMime)) {
                    log.warn("MIME mismatch: detected={}, declared={}", detectedMime, declaredMime);
                    throw new SecurityException(
                            "El tipo de archivo no coincide: declarado " + declaredMime +
                            ", real " + detectedMime);
                }
            }
        } catch (SecurityException e) {
            throw e;
        } catch (IOException e) {
            log.warn("No se pudo validar MIME type: {}", e.getMessage());
            // Si no podemos leer el stream, permitimos pero loggeamos
        }
    }

    /**
     * Detecta MIME type por magic bytes.
     */
    private String detectMime(byte[] header) {
        for (Map.Entry<String, byte[]> entry : MIME_SIGNATURES.entrySet()) {
            if (startsWith(header, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Verifica si dos MIME types son compatibles.
     * Ejemplo: image/jpeg es compatible con image/*
     */
    private boolean isCompatibleMime(String detected, String declared) {
        // Coincidencia exacta
        if (detected.equalsIgnoreCase(declared)) return true;

        // Wildcard: image/*, application/*
        String detectedBase = detected.substring(0, detected.indexOf('/'));
        String declaredBase = declared.substring(0, declared.indexOf('/'));
        if (declared.endsWith("/*") && detectedBase.equalsIgnoreCase(declaredBase)) return true;

        return false;
    }

    /**
     * Previene path traversal: no permitir .., /, \ en el nombre.
     */
    private void validatePathTraversal(String filename) {
        if (filename == null) return;

        String normalized = Path.of(filename).getFileName().toString();
        if (normalized.contains("..") || normalized.contains("/") || normalized.contains("\\")) {
            throw new SecurityException("Nombre de archivo contiene caracteres no seguros");
        }
    }

    /**
     * Extrae la extensión del archivo (sin punto).
     */
    public static String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }

    /**
     * Verifica si un byte array empieza con los bytes dados.
     */
    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
