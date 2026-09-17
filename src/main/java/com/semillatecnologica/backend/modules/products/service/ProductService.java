package com.semillatecnologica.backend.modules.products.service;

import com.semillatecnologica.backend.modules.admin.service.AuditService;
import com.semillatecnologica.backend.modules.products.dto.ProductResponse;
import com.semillatecnologica.backend.modules.products.model.Product;
import com.semillatecnologica.backend.modules.products.repository.ProductRepository;
import com.semillatecnologica.backend.modules.storage.service.StorageService;
import com.semillatecnologica.backend.shared.exception.NotFoundException;
import com.semillatecnologica.backend.shared.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de productos.
 *
 * <p>La imagen es OBLIGATORIA al crear un producto: se sube
 * internamente via {@link StorageService} y se guarda la referencia.
 * Cada operación de escritura se registra en auditoría (quién, qué, cuándo).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final StorageService storageService;
    private final AuditService auditService;

    /**
     * Lista productos con stock (tienda pública).
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> listAvailable() {
        return productRepository.findByStockGreaterThanOrderByCreatedAtDesc(0)
                .stream()
                .map(p -> ProductResponse.from(p, resolveImageUrl(p)))
                .toList();
    }

    /**
     * Lista todos los productos (admin).
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> listAll() {
        return productRepository.findAll()
                .stream()
                .map(p -> ProductResponse.from(p, resolveImageUrl(p)))
                .toList();
    }

    /**
     * Obtiene un producto por ID.
     */
    @Transactional(readOnly = true)
    public ProductResponse get(String id) {
        Product product = findById(id);
        return ProductResponse.from(product, resolveImageUrl(product));
    }

    /**
     * Crea un producto con imagen obligatoria.
     *
     * @param name Nombre
     * @param description Descripción
     * @param price Precio (MXN)
     * @param category Categoría
     * @param stock Stock
     * @param image Imagen (obligatoria)
     * @param userId ID del usuario que crea
     * @return Producto creado
     */
    @Transactional
    public ProductResponse create(String name, String description, BigDecimal price,
                                   String category, int stock,
                                   MultipartFile image, String userId,
                                   HttpServletRequest request) {
        if (image == null || image.isEmpty()) {
            throw new ValidationException("La imagen es obligatoria");
        }
        validatePrice(price);

        // Subir imagen internamente
        var uploaded = storageService.upload(image, userId, "PRODUCT", null);

        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .description(description)
                .price(price)
                .category(category)
                .stock(stock)
                .imageFileId(uploaded.id())
                .build();

        productRepository.save(product);

        auditService.log("PRODUCT_CREATED", "PRODUCT", product.getId(), userId,
                null, productToMap(product), request);

        log.info("Producto creado: {} — ${}", name, price);
        return ProductResponse.from(product, uploaded.url());
    }

    /**
     * Actualiza un producto. La imagen es opcional en update.
     */
    @Transactional
    public ProductResponse update(String id, String name, String description, BigDecimal price,
                                   String category, Integer stock,
                                   MultipartFile image, String userId,
                                   HttpServletRequest request) {
        Product product = findById(id);
        Map<String, Object> before = productToMap(product);

        if (name != null) product.setName(name);
        if (description != null) product.setDescription(description);
        if (price != null) {
            validatePrice(price);
            product.setPrice(price);
        }
        if (category != null) product.setCategory(category);
        if (stock != null) product.setStock(stock);

        // Si se provee nueva imagen, subirla y reemplazar la referencia
        if (image != null && !image.isEmpty()) {
            var uploaded = storageService.upload(image, userId, "PRODUCT", product.getId());
            product.setImageFileId(uploaded.id());
        }

        productRepository.save(product);

        auditService.log("PRODUCT_UPDATED", "PRODUCT", product.getId(), userId,
                before, productToMap(product), request);

        log.info("Producto actualizado: {}", product.getName());
        return ProductResponse.from(product, resolveImageUrl(product));
    }

    /**
     * Elimina un producto.
     */
    @Transactional
    public void delete(String id, String userId, HttpServletRequest request) {
        Product product = findById(id);
        productRepository.delete(product);

        auditService.log("PRODUCT_DELETED", "PRODUCT", product.getId(), userId,
                productToMap(product), null, request);

        log.info("Producto eliminado: {}", product.getName());
    }

    private Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto"));
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El precio debe ser mayor a cero");
        }
    }

    private String resolveImageUrl(Product product) {
        if (product.getImageFileId() == null) return null;
        return storageService.getAccessUrl(product.getImageFileId(), 0).orElse(null);
    }

    /**
     * Convierte un producto a un mapa para auditoría (sin datos sensibles).
     */
    private Map<String, Object> productToMap(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", product.getName());
        map.put("price", product.getPrice());
        map.put("category", product.getCategory());
        map.put("stock", product.getStock());
        return map;
    }
}