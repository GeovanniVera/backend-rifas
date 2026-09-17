package com.semillatecnologica.backend.modules.raffles.service;

import com.semillatecnologica.backend.modules.admin.service.AuditService;
import com.semillatecnologica.backend.modules.products.model.Product;
import com.semillatecnologica.backend.modules.products.repository.ProductRepository;
import com.semillatecnologica.backend.modules.raffles.dto.RaffleResponse;
import com.semillatecnologica.backend.modules.raffles.model.Raffle;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleRepository;
import com.semillatecnologica.backend.modules.storage.service.StorageService;
import com.semillatecnologica.backend.shared.exception.NotFoundException;
import com.semillatecnologica.backend.shared.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de rifas.
 *
 * <p>Al crear una rifa se aparta el producto del stock de venta (stock = 0).
 * El precio por boleto se calcula con {@link BigDecimal}:
 * {@code ticketPrice = (product.price × (1 + margin/100)) / ticketCount}</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaffleService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final RaffleRepository raffleRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;
    private final AuditService auditService;

    /**
     * Lista rifas activas (tienda pública).
     */
    @Transactional(readOnly = true)
    public List<RaffleResponse> listActive() {
        return raffleRepository.findByStatusOrderByCreatedAtDesc(Raffle.Status.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lista todas las rifas (admin).
     */
    @Transactional(readOnly = true)
    public List<RaffleResponse> listAll() {
        return raffleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Obtiene una rifa por ID.
     */
    @Transactional(readOnly = true)
    public RaffleResponse get(String id) {
        return toResponse(findById(id));
    }

    /**
     * Crea una rifa a partir de un producto.
     *
     * <p>El producto debe tener stock. Se aparta del stock de venta
     * y se calcula el precio por boleto con margen configurable.</p>
     */
    @Transactional
    public RaffleResponse create(String productId, BigDecimal marginPercent, int ticketCount,
                                  String userId, HttpServletRequest request) {
        if (ticketCount <= 0) {
            throw new ValidationException("La cantidad de boletos debe ser mayor a cero");
        }
        if (marginPercent == null || marginPercent.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El margen no puede ser negativo");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Producto"));

        if (product.getStock() <= 0) {
            throw new ValidationException("El producto no tiene stock para rifar");
        }

        // Cálculo con BigDecimal: (price × (1 + margin/100)) / ticketCount
        BigDecimal total = product.getPrice()
                .multiply(BigDecimal.ONE.add(marginPercent.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP)));
        BigDecimal ticketPrice = total.divide(BigDecimal.valueOf(ticketCount), 2, RoundingMode.HALF_UP);

        Raffle raffle = Raffle.builder()
                .id(UUID.randomUUID().toString())
                .productId(product.getId())
                .name("Rifa — " + product.getName())
                .description("Rifá tu " + product.getName() + " por boletos. El producto se aparta del stock de venta.")
                .marginPercent(marginPercent)
                .ticketCount(ticketCount)
                .ticketPrice(ticketPrice)
                .status(Raffle.Status.ACTIVE)
                .build();

        raffleRepository.save(raffle);

        // Apartar el producto del stock de venta
        product.setStock(0);
        productRepository.save(product);

        auditService.log("RAFFLE_CREATED", "RAFFLE", raffle.getId(), userId,
                null, raffleToMap(raffle), request);

        log.info("Rifa creada: {} — ${} por boleto, {} boletos",
                raffle.getName(), ticketPrice, ticketCount);

        return toResponse(raffle);
    }

    /**
     * Cambia el estado de una rifa (ACTIVE → DRAW → FINISHED, o CANCELLED).
     */
    @Transactional
    public RaffleResponse changeStatus(String id, Raffle.Status newStatus,
                                        String userId, HttpServletRequest request) {
        Raffle raffle = findById(id);
        Raffle.Status oldStatus = raffle.getStatus();

        validateStatusTransition(raffle, newStatus);

        raffle.setStatus(newStatus);
        raffleRepository.save(raffle);

        auditService.log("RAFFLE_STATUS_CHANGED", "RAFFLE", raffle.getId(), userId,
                Map.of("status", oldStatus.name()), Map.of("status", newStatus.name()), request);

        log.info("Rifa {}: estado {} → {}", raffle.getName(), oldStatus, newStatus);
        return toResponse(raffle);
    }

    /**
     * Valida las transiciones de estado permitidas.
     *
     * <ul>
     *   <li>ACTIVE → DRAW: avanza a sorteo</li>
     *   <li>ACTIVE → CANCELLED: solo si NO hay boletos vendidos</li>
     *   <li>DRAW → FINISHED: sorteo realizado</li>
     *   <li>FINISHED / CANCELLED: terminales, no se cambian</li>
     * </ul>
     */
    private void validateStatusTransition(Raffle raffle, Raffle.Status newStatus) {
        Raffle.Status current = raffle.getStatus();

        if (current == newStatus) return;

        switch (current) {
            case ACTIVE -> {
                if (newStatus == Raffle.Status.DRAW) return;
                if (newStatus == Raffle.Status.CANCELLED) {
                    if (raffle.getTicketsSold() > 0) {
                        throw new ValidationException(
                                "No se puede cancelar una rifa con boletos vendidos");
                    }
                    return;
                }
                throw new ValidationException("Transición inválida desde ACTIVE: " + newStatus);
            }
            case DRAW -> {
                if (newStatus == Raffle.Status.FINISHED) return;
                throw new ValidationException(
                        "Una rifa en sorteo solo puede avanzar a FINISHED");
            }
            case FINISHED, CANCELLED ->
                    throw new ValidationException(
                            "La rifa " + current.name().toLowerCase() + " es un estado terminal");
        }
    }

    /**
     * Elimina una rifa. Solo si no está en sorteo ni finalizada.
     */
    @Transactional
    public void delete(String id, String userId, HttpServletRequest request) {
        Raffle raffle = findById(id);

        if (raffle.getStatus() == Raffle.Status.DRAW || raffle.getStatus() == Raffle.Status.FINISHED) {
            throw new ValidationException("No se puede eliminar una rifa en sorteo o finalizada");
        }

        raffleRepository.delete(raffle);

        auditService.log("RAFFLE_DELETED", "RAFFLE", raffle.getId(), userId,
                raffleToMap(raffle), null, request);

        log.info("Rifa eliminada: {}", raffle.getName());
    }

    private Raffle findById(String id) {
        return raffleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rifa"));
    }

    private RaffleResponse toResponse(Raffle raffle) {
        Product product = productRepository.findById(raffle.getProductId()).orElse(null);
        String productName = product != null ? product.getName() : null;
        String imageUrl = product != null && product.getImageFileId() != null
                ? storageService.getAccessUrl(product.getImageFileId(), 0).orElse(null)
                : null;
        return RaffleResponse.from(raffle, productName, imageUrl);
    }

    private Map<String, Object> raffleToMap(Raffle raffle) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", raffle.getName());
        map.put("productId", raffle.getProductId());
        map.put("marginPercent", raffle.getMarginPercent());
        map.put("ticketCount", raffle.getTicketCount());
        map.put("ticketsSold", raffle.getTicketsSold());
        map.put("ticketPrice", raffle.getTicketPrice());
        map.put("status", raffle.getStatus().name());
        return map;
    }
}