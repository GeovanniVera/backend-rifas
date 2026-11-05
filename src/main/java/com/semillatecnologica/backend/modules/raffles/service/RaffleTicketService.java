package com.semillatecnologica.backend.modules.raffles.service;

import com.semillatecnologica.backend.modules.admin.service.AuditService;
import com.semillatecnologica.backend.modules.payments.service.PaymentService;
import com.semillatecnologica.backend.modules.payments.strategy.PaymentIntent;
import com.semillatecnologica.backend.modules.raffles.dto.CheckoutIntentResponse;
import com.semillatecnologica.backend.modules.raffles.dto.CreateCheckoutIntentRequest;
import com.semillatecnologica.backend.modules.raffles.dto.RaffleTicketResponse;
import com.semillatecnologica.backend.modules.raffles.model.Raffle;
import com.semillatecnologica.backend.modules.raffles.model.RaffleTicket;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleRepository;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleTicketRepository;
import com.semillatecnologica.backend.shared.exception.NotFoundException;
import com.semillatecnologica.backend.shared.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaffleTicketService {

    private final RaffleRepository raffleRepository;
    private final RaffleTicketRepository ticketRepository;
    private final PaymentService paymentService;
    private final AuditService auditService;

    private static final int RESERVATION_EXPIRATION_MINUTES = 10;

    /**
     * Genera los boletos del 1 al N para una rifa si aún no existen.
     */
    @Transactional
    public List<RaffleTicket> generateTicketsForRaffle(Raffle raffle) {
        if (ticketRepository.existsByRaffleId(raffle.getId())) {
            return ticketRepository.findByRaffleIdOrderByTicketNumberAsc(raffle.getId());
        }

        List<RaffleTicket> tickets = new ArrayList<>();
        for (int i = 1; i <= raffle.getTicketCount(); i++) {
            tickets.add(RaffleTicket.builder()
                    .id(UUID.randomUUID().toString())
                    .raffleId(raffle.getId())
                    .ticketNumber(i)
                    .status(RaffleTicket.Status.AVAILABLE)
                    .build());
        }

        log.info("Generando {} boletos para la rifa {}", tickets.size(), raffle.getName());
        return ticketRepository.saveAll(tickets);
    }

    /**
     * Obtiene todos los boletos de una rifa.
     * Si no se habían generado, los inicializa.
     * Libera boletos con reserva expirada (>10 min).
     */
    @Transactional
    public List<RaffleTicketResponse> getTicketsForRaffle(String raffleId, String currentUserId) {
        Raffle raffle = raffleRepository.findById(raffleId)
                .orElseThrow(() -> new NotFoundException("Rifa"));

        List<RaffleTicket> tickets = ticketRepository.findByRaffleIdOrderByTicketNumberAsc(raffleId);
        if (tickets.isEmpty()) {
            tickets = generateTicketsForRaffle(raffle);
        } else {
            // Liberar reservas expiradas
            LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(RESERVATION_EXPIRATION_MINUTES);
            boolean modified = false;
            for (RaffleTicket t : tickets) {
                if (t.getStatus() == RaffleTicket.Status.RESERVED &&
                        t.getReservedAt() != null &&
                        t.getReservedAt().isBefore(expirationThreshold)) {
                    t.setStatus(RaffleTicket.Status.AVAILABLE);
                    t.setPaymentIntentId(null);
                    t.setUserId(null);
                    t.setReservedAt(null);
                    modified = true;
                }
            }
            if (modified) {
                ticketRepository.saveAll(tickets);
            }
        }

        return tickets.stream()
                .map(t -> RaffleTicketResponse.from(t, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Crea un PaymentIntent en Stripe y reserva los boletos seleccionados.
     */
    @Transactional
    public CheckoutIntentResponse createCheckoutIntent(CreateCheckoutIntentRequest request,
                                                       String userId,
                                                       HttpServletRequest httpRequest) {
        Raffle raffle = raffleRepository.findById(request.raffleId())
                .orElseThrow(() -> new NotFoundException("Rifa"));

        if (raffle.getStatus() != Raffle.Status.ACTIVE ||
                (raffle.getDrawDate() != null && raffle.getDrawDate().isBefore(LocalDateTime.now()))) {
            if (raffle.getStatus() == Raffle.Status.ACTIVE) {
                raffle.setStatus(Raffle.Status.DRAW);
                raffleRepository.save(raffle);
            }
            throw new ValidationException("La fecha límite para participar en esta rifa ya expiró");
        }

        List<Integer> numbers = request.ticketNumbers();
        if (numbers == null || numbers.isEmpty()) {
            throw new ValidationException("Debes seleccionar al menos un boleto");
        }

        // Evitar números duplicados en la petición
        Set<Integer> uniqueNumbers = new HashSet<>(numbers);
        if (uniqueNumbers.size() != numbers.size()) {
            throw new ValidationException("No puedes seleccionar boletos duplicados");
        }

        // Asegurarse de que existan los boletos
        if (!ticketRepository.existsByRaffleId(raffle.getId())) {
            generateTicketsForRaffle(raffle);
        }

        List<RaffleTicket> selectedTickets = ticketRepository.findByRaffleIdAndTicketNumberIn(
                raffle.getId(), uniqueNumbers);

        if (selectedTickets.size() != uniqueNumbers.size()) {
            throw new ValidationException("Uno o más números de boleto son inválidos para esta rifa");
        }

        LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(RESERVATION_EXPIRATION_MINUTES);

        // Validación de exclusión mutua
        for (RaffleTicket ticket : selectedTickets) {
            if (ticket.getStatus() == RaffleTicket.Status.SOLD) {
                throw new ValidationException("El boleto #" + ticket.getTicketNumber() + " ya fue vendido a otro usuario");
            }
            if (ticket.getStatus() == RaffleTicket.Status.RESERVED) {
                boolean isExpired = ticket.getReservedAt() != null && ticket.getReservedAt().isBefore(expirationThreshold);
                boolean isSameUser = userId.equals(ticket.getUserId());
                if (!isExpired && !isSameUser) {
                    throw new ValidationException("El boleto #" + ticket.getTicketNumber() +
                            " está siendo procesado por otro usuario. Intenta de nuevo en unos minutos");
                }
            }
        }

        // Calcular total a pagar
        BigDecimal ticketPrice = raffle.getTicketPrice();
        BigDecimal totalAmount = ticketPrice.multiply(BigDecimal.valueOf(selectedTickets.size()));

        // En Stripe el monto va en centavos (ej: $50.00 MXN -> 5000 centavos)
        long amountInCents = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, String> metadata = Map.of(
                "raffleId", raffle.getId(),
                "raffleName", raffle.getName(),
                "userId", userId,
                "ticketNumbers", numbers.stream().map(String::valueOf).collect(Collectors.joining(","))
        );

        // Crear cobro a través de PaymentService / Stripe
        PaymentIntent intent = paymentService.createPayment(
                userId,
                amountInCents,
                "mxn",
                "Compra de " + selectedTickets.size() + " boleto(s) — " + raffle.getName(),
                metadata
        );

        // Reservar los boletos con el PaymentIntentId
        LocalDateTime now = LocalDateTime.now();
        for (RaffleTicket ticket : selectedTickets) {
            ticket.setStatus(RaffleTicket.Status.RESERVED);
            ticket.setPaymentIntentId(intent.gatewayId());
            ticket.setUserId(userId);
            ticket.setReservedAt(now);
        }
        ticketRepository.saveAll(selectedTickets);

        log.info("Intento de checkout creado: pi={} usuario={} boletos={}",
                intent.gatewayId(), userId, numbers);

        return new CheckoutIntentResponse(
                intent.clientSecret(),
                intent.gatewayId(),
                amountInCents,
                totalAmount,
                "mxn",
                numbers
        );
    }

    /**
     * Confirma la compra tras el éxito en Stripe (Etapa 4).
     * Marca los boletos como SOLD y actualiza el contador de la rifa.
     */
    @Transactional
    public List<RaffleTicketResponse> confirmPurchase(String paymentIntentId,
                                                      String userId,
                                                      HttpServletRequest httpRequest) {
        List<RaffleTicket> tickets = ticketRepository.findByPaymentIntentId(paymentIntentId);

        if (tickets.isEmpty()) {
            throw new NotFoundException("No se encontraron boletos asociados al pago " + paymentIntentId);
        }

        RaffleTicket sampleTicket = tickets.get(0);
        Raffle raffle = raffleRepository.findById(sampleTicket.getRaffleId())
                .orElseThrow(() -> new NotFoundException("Rifa"));

        LocalDateTime now = LocalDateTime.now();
        for (RaffleTicket ticket : tickets) {
            ticket.setStatus(RaffleTicket.Status.SOLD);
            ticket.setUserId(userId);
            ticket.setPurchasedAt(now);
        }
        ticketRepository.saveAll(tickets);

        // Actualizar ticketsSold en la entidad Raffle
        long totalSold = ticketRepository.countByRaffleIdAndStatus(raffle.getId(), RaffleTicket.Status.SOLD);
        raffle.setTicketsSold((int) totalSold);

        // Si se vendieron todos los boletos, pasa a DRAW
        if (raffle.getTicketsSold() >= raffle.getTicketCount()) {
            raffle.setStatus(Raffle.Status.DRAW);
            log.info("Rifa {} vendió todos sus boletos y avanza a DRAW", raffle.getName());
        }
        raffleRepository.save(raffle);

        auditService.log("TICKETS_PURCHASED", "RAFFLE", raffle.getId(), userId,
                null, Map.of(
                        "paymentIntentId", paymentIntentId,
                        "tickets", tickets.stream().map(RaffleTicket::getTicketNumber).toList(),
                        "totalSold", raffle.getTicketsSold()
                ), httpRequest);

        log.info("Compra confirmada: pi={} usuario={} boletos={}",
                paymentIntentId, userId,
                tickets.stream().map(RaffleTicket::getTicketNumber).toList());

        return tickets.stream()
                .map(t -> RaffleTicketResponse.from(t, userId))
                .collect(Collectors.toList());
    }
}
