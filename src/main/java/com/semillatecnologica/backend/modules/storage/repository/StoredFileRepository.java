package com.semillatecnologica.backend.modules.storage.repository;

import com.semillatecnologica.backend.modules.storage.model.StoredFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de archivos almacenados.
 */
@Repository
public interface StoredFileRepository extends JpaRepository<StoredFileEntity, String> {

    /**
     * Busca archivos vinculados a una entidad específica.
     */
    List<StoredFileEntity> findByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * Busca archivos de un usuario.
     */
    List<StoredFileEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Cuenta archivos de un usuario.
     */
    long countByUserId(String userId);
}
