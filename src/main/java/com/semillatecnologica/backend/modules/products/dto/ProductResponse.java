package com.semillatecnologica.backend.modules.products.dto;

import com.semillatecnologica.backend.modules.products.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta de un producto.
 */
@Schema(description = "Producto de la tienda")
public record ProductResponse(
    String id,
    String name,
    String description,
    BigDecimal price,
    String category,
    int stock,
    String imageUrl,
    LocalDateTime createdAt
) {
    public static ProductResponse from(Product product, String imageUrl) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getStock(),
                imageUrl,
                product.getCreatedAt()
        );
    }
}