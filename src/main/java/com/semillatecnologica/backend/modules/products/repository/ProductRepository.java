package com.semillatecnologica.backend.modules.products.repository;

import com.semillatecnologica.backend.modules.products.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de productos.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Productos con stock disponible (tienda pública).
     */
    List<Product> findByStockGreaterThanOrderByCreatedAtDesc(int stock);

    /**
     * Productos por categoría.
     */
    List<Product> findByCategoryOrderByName(String category);
}