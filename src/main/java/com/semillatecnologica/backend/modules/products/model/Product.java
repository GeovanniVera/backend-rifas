package com.semillatecnologica.backend.modules.products.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Producto de tecnología de la tienda.
 *
 * <p>El precio se maneja con {@link BigDecimal} para precisión
 * monetaria exacta. La imagen se referencia por storageKey
 * (el binario vive en el módulo de almacenamiento).</p>
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private int stock = 0;

    /**
     * ID del archivo de imagen en stored_files (imagen obligatoria).
     */
    @Column(name = "image_file_id", length = 36)
    private String imageFileId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}