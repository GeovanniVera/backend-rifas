package com.semillatecnologica.backend.modules.storage.strategy;

import org.springframework.web.multipart.MultipartFile;

/**
 * Estrategia de almacenamiento de archivos.
 *
 * <p>Define el contrato común para persistir, eliminar y acceder
 * a archivos. Cada implementación encapsula un proveedor.</p>
 */
public interface IStorageStrategy {

    /**
     * Sube un archivo y retorna la metadata almacenada.
     *
     * @param file Archivo multipart
     * @param storageKey Clave de almacenamiento generada por el servicio
     * @return StoredFile con la metadata del archivo almacenado
     */
    StoredFile upload(MultipartFile file, String storageKey);

    /**
     * Elimina un archivo por su clave de almacenamiento.
     *
     * @param storageKey Clave del archivo a eliminar
     */
    void delete(String storageKey);

    /**
     * Retorna una URL de acceso al archivo.
     *
     * <p>Para archivos privados, retorna una URL firmada con expiración.
     * Para archivos públicos, retorna la URL permanente.</p>
     *
     * @param storageKey Clave del archivo
     * @param expiresInMinutes Minutos de expiración (0 = sin expiración)
     * @return URL de acceso
     */
    String getAccessUrl(String storageKey, int expiresInMinutes);

    /**
     * Retorna el identificador del target de almacenamiento.
     */
    String getTarget();
}
