package com.semillatecnologica.backend.modules.raffles.controller;

import com.semillatecnologica.backend.modules.raffles.dto.RaffleResponse;
import com.semillatecnologica.backend.modules.raffles.model.Raffle;
import com.semillatecnologica.backend.modules.raffles.service.RaffleService;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador de rifas.
 *
 * <p>La tienda pública lee rifas activas sin autenticación.
 * El CRUD administrativo requiere permisos.</p>
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Rifas", description = "Rifas de productos de la tienda")
public class RaffleController {

    private final RaffleService raffleService;

    /**
     * Lista rifas activas (tienda pública).
     */
    @GetMapping("/raffles")
    @Operation(summary = "Listar rifas activas",
               description = "Público — rifas activas para la tienda.")
    public ResponseEntity<ApiResponse<List<RaffleResponse>>> listActive() {
        List<RaffleResponse> raffles = raffleService.listActive();
        return ResponseEntity.ok(ApiResponse.ok("Rifas activas", raffles));
    }

    /**
     * Lista todas las rifas (admin).
     */
    @GetMapping("/admin/raffles")
    @PreAuthorize("hasAuthority('raffles.read')")
    @Operation(summary = "Listar todas las rifas",
               description = "Requiere permiso `raffles.read`.")
    public ResponseEntity<ApiResponse<List<RaffleResponse>>> listAll() {
        List<RaffleResponse> raffles = raffleService.listAll();
        return ResponseEntity.ok(ApiResponse.ok("Rifas", raffles));
    }

    /**
     * Obtiene una rifa por ID.
     */
    @GetMapping("/raffles/{id}")
    @Operation(summary = "Obtener rifa",
               description = "Público — detalle de una rifa.")
    public ResponseEntity<ApiResponse<RaffleResponse>> get(@PathVariable String id) {
        RaffleResponse raffle = raffleService.get(id);
        return ResponseEntity.ok(ApiResponse.ok("Rifa", raffle));
    }

    /**
     * Crea una rifa a partir de un producto.
     */
    @PostMapping("/admin/raffles")
    @PreAuthorize("hasAuthority('raffles.write')")
    @Operation(summary = "Crear rifa",
               description = "Requiere `raffles.write`. Aparta el producto del stock.")
    public ResponseEntity<ApiResponse<RaffleResponse>> create(
            @RequestParam String productId,
            @RequestParam(defaultValue = "7") BigDecimal marginPercent,
            @RequestParam int ticketCount,
            Authentication authentication,
            HttpServletRequest request) {

        RaffleResponse raffle = raffleService.create(
                productId, marginPercent, ticketCount, authentication.getName(), request);

        return ResponseEntity.ok(ApiResponse.ok("Rifa creada", raffle));
    }

    /**
     * Cambia el estado de una rifa.
     */
    @PatchMapping("/admin/raffles/{id}/status")
    @PreAuthorize("hasAuthority('raffles.write')")
    @Operation(summary = "Cambiar estado de rifa",
               description = "Requiere `raffles.write`. ACTIVE → DRAW → FINISHED, o CANCELLED.")
    public ResponseEntity<ApiResponse<RaffleResponse>> changeStatus(
            @PathVariable String id,
            @RequestParam Raffle.Status status,
            Authentication authentication,
            HttpServletRequest request) {

        RaffleResponse raffle = raffleService.changeStatus(
                id, status, authentication.getName(), request);

        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado", raffle));
    }

    /**
     * Elimina una rifa (solo si no está en sorteo ni finalizada).
     */
    @DeleteMapping("/admin/raffles/{id}")
    @PreAuthorize("hasAuthority('raffles.write')")
    @Operation(summary = "Eliminar rifa",
               description = "Requiere `raffles.write`. No permite eliminar en sorteo/finalizada.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            Authentication authentication,
            HttpServletRequest request) {

        raffleService.delete(id, authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Rifa eliminada"));
    }
}