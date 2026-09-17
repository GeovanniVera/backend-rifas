package com.semillatecnologica.backend.modules.products.controller;

import com.semillatecnologica.backend.modules.products.dto.ProductResponse;
import com.semillatecnologica.backend.modules.products.service.ProductService;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador de productos.
 *
 * <p>La tienda pública lee productos sin autenticación.
 * El CRUD administrativo requiere permisos.</p>
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Catálogo de productos de la tienda")
public class ProductController {

    private final ProductService productService;

    /**
     * Lista productos disponibles (tienda pública).
     */
    @GetMapping("/products")
    @Operation(summary = "Listar productos disponibles",
               description = "Público — productos con stock para la tienda.")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listAvailable() {
        List<ProductResponse> products = productService.listAvailable();
        return ResponseEntity.ok(ApiResponse.ok("Productos disponibles", products));
    }

    /**
     * Lista todos los productos (admin).
     */
    @GetMapping("/admin/products")
    @PreAuthorize("hasAuthority('products.read')")
    @Operation(summary = "Listar todos los productos",
               description = "Requiere permiso `products.read`.")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listAll() {
        List<ProductResponse> products = productService.listAll();
        return ResponseEntity.ok(ApiResponse.ok("Productos", products));
    }

    /**
     * Obtiene un producto por ID.
     */
    @GetMapping("/products/{id}")
    @Operation(summary = "Obtener producto",
               description = "Público — detalle de un producto.")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable String id) {
        ProductResponse product = productService.get(id);
        return ResponseEntity.ok(ApiResponse.ok("Producto", product));
    }

    /**
     * Crea un producto con imagen OBLIGATORIA (multipart).
     */
    @PostMapping(value = "/admin/products", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAuthority('products.write')")
    @Operation(summary = "Crear producto",
               description = "Requiere `products.write`. La imagen es obligatoria.")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @RequestParam("name") String name,
            @RequestParam(required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("category") String category,
            @RequestParam(defaultValue = "0") int stock,
            @RequestParam("image") MultipartFile image,
            Authentication authentication,
            HttpServletRequest request) {

        ProductResponse product = productService.create(
                name, description, price, category, stock, image, authentication.getName(), request);

        return ResponseEntity.ok(ApiResponse.ok("Producto creado", product));
    }

    /**
     * Actualiza un producto. La imagen es opcional.
     */
    @PutMapping(value = "/admin/products/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAuthority('products.write')")
    @Operation(summary = "Actualizar producto",
               description = "Requiere `products.write`. La imagen es opcional al editar.")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) MultipartFile image,
            Authentication authentication,
            HttpServletRequest request) {

        ProductResponse product = productService.update(
                id, name, description, price, category, stock, image, authentication.getName(), request);

        return ResponseEntity.ok(ApiResponse.ok("Producto actualizado", product));
    }

    /**
     * Elimina un producto.
     */
    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasAuthority('products.write')")
    @Operation(summary = "Eliminar producto",
               description = "Requiere `products.write`.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            Authentication authentication,
            HttpServletRequest request) {
        productService.delete(id, authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Producto eliminado"));
    }
}