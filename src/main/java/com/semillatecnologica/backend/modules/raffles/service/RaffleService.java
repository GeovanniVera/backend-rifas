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
import org.springframework.beans.factory.annotation.Value;
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
 * <p>
 * Al crear una rifa se aparta el producto del stock de venta (stock = 0).
 * El precio por boleto se calcula aplicando la regla RN-RIFA-001 (7% del costo,
 * 29 boletos fijos).
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaffleService {

    private final RaffleRepository raffleRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;
    private final AuditService auditService;

    // Configuración inyectada desde application.yaml
    @Value("${app.raffles.ticket-rate:0.07}")
    private BigDecimal ticketRate;

    @Value("${app.raffles.min-ticket-price:1.00}")
    private BigDecimal minTicketPrice;

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
     * Crea una rifa a partir de un producto (Regla RN-RIFA-001).
     *
     * <p>
     * El producto debe tener stock. Se aparta del stock de venta (stock = 0)
     * y el precio del boleto se calcula en base a la tasa configurada. La cantidad
     * de boletos (N) se calcula dinámicamente como ceil(2 / ticketRate).
     * </p>
     */
    @Transactional
    public RaffleResponse create(String productId, String userId, HttpServletRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Producto"));

        if (product.getStock() <= 0) {
            throw new ValidationException("El producto no tiene stock para rifar");
        }

        // Cálculo dinámico de total de boletos: ceil(2.0 / ticketRate)
        int totalTickets = (int) Math.ceil(2.0 / ticketRate.doubleValue());

        // Cálculo del precio por boleto (redondeado a entero)
        BigDecimal ticketPrice = product.getPrice()
                .multiply(ticketRate)
                .setScale(0, RoundingMode.HALF_UP);

        if (ticketPrice.compareTo(minTicketPrice) < 0) {
            throw new ValidationException("El precio del boleto no puede ser menor a $" + minTicketPrice + " MXN");
        }

        Raffle raffle = Raffle.builder()
                .id(UUID.randomUUID().toString())
                .productId(product.getId())
                .name("Rifa — " + product.getName())
                .description(
                        "Rifa de " + product.getName() + " (" + totalTickets + " boletos de $" + ticketPrice + " MXN)")
                .marginPercent(new BigDecimal("100.00"))
                .ticketCount(totalTickets)
                .ticketPrice(ticketPrice)
                .status(Raffle.Status.ACTIVE)
                .build();

        raffleRepository.save(raffle);

        // Apartar el producto del stock de venta
        product.setStock(0);
        productRepository.save(product);

        auditService.log("RAFFLE_CREATED", "RAFFLE", raffle.getId(), userId,
                null, raffleToMap(raffle), request);

        log.info("Rifa creada (RN-RIFA-001): {} — ${} por boleto, {} boletos",
                raffle.getName(), ticketPrice, totalTickets);

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
     */
    private void validateStatusTransition(Raffle raffle, Raffle.Status newStatus) {
        Raffle.Status current = raffle.getStatus();

        if (current == newStatus)
            return;

        switch (current) {
            case ACTIVE -> {
                if (newStatus == Raffle.Status.DRAW)
                    return;
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
                if (newStatus == Raffle.Status.FINISHED)
                    return;
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