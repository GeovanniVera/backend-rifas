package com.semillatecnologica.backend.modules.raffles.service;

import com.semillatecnologica.backend.modules.admin.service.AuditService;
import com.semillatecnologica.backend.modules.auth.model.User;
import com.semillatecnologica.backend.modules.auth.repository.UserRepository;
import com.semillatecnologica.backend.modules.notification.inapp.service.InAppNotificationService;
import com.semillatecnologica.backend.modules.notification.service.NotificationService;
import com.semillatecnologica.backend.modules.payments.service.PaymentService;
import com.semillatecnologica.backend.modules.payments.strategy.PaymentIntent;
import com.semillatecnologica.backend.modules.raffles.dto.CheckoutIntentResponse;
import com.semillatecnologica.backend.modules.raffles.dto.CreateCheckoutIntentRequest;
import com.semillatecnologica.backend.modules.raffles.dto.OxxoReservationResponse;
import com.semillatecnologica.backend.modules.raffles.dto.PendingOxxoReservationDto;
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
    private final InAppNotificationService inAppNotificationService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private static final int CARD_RESERVATION_EXPIRATION_MINUTES = 10;
    private static final int OXXO_RESERVATION_EXPIRATION_HOURS = 3;

    /**
     * Verifica si una reserva de boleto ha expirado.
     * Si cuenta con expiresAt explícito se compara con esa fecha (ej: 3 horas para OXXO o 10 min para tarjeta).
     * De lo contrario, usa el umbral base de 10 minutos.
     */
    private boolean isTicketReservationExpired(RaffleTicket ticket, LocalDateTime now) {
        if (ticket.getStatus() != RaffleTicket.Status.RESERVED) {
            return false;
        }
        if (ticket.getExpiresAt() != null) {
            return now.isAfter(ticket.getExpiresAt());
        }
        if (ticket.getReservedAt() != null) {
            return ticket.getReservedAt().isBefore(now.minusMinutes(CARD_RESERVATION_EXPIRATION_MINUTES));
        }
        return false;
    }

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
     * Libera boletos con reserva expirada (10 min para tarjeta, 3 horas para OXXO).
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
            LocalDateTime now = LocalDateTime.now();
            boolean modified = false;
            for (RaffleTicket t : tickets) {
                if (isTicketReservationExpired(t, now)) {
                    t.setStatus(RaffleTicket.Status.AVAILABLE);
                    t.setPaymentIntentId(null);
                    t.setUserId(null);
                    t.setReservedAt(null);
                    t.setExpiresAt(null);
                    t.setPaymentMethod(null);
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

        LocalDateTime now = LocalDateTime.now();

        // Validación de exclusión mutua
        for (RaffleTicket ticket : selectedTickets) {
            if (ticket.getStatus() == RaffleTicket.Status.SOLD) {
                throw new ValidationException("El boleto #" + ticket.getTicketNumber() + " ya fue vendido a otro usuario");
            }
            if (ticket.getStatus() == RaffleTicket.Status.RESERVED) {
                boolean isExpired = isTicketReservationExpired(ticket, now);
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

        // Reservar los boletos con el PaymentIntentId (10 min iniciales para tarjeta)
        LocalDateTime initialExpiresAt = now.plusMinutes(CARD_RESERVATION_EXPIRATION_MINUTES);
        for (RaffleTicket ticket : selectedTickets) {
            ticket.setStatus(RaffleTicket.Status.RESERVED);
            ticket.setPaymentIntentId(intent.gatewayId());
            ticket.setUserId(userId);
            ticket.setPaymentMethod("CARD");
            ticket.setReservedAt(now);
            ticket.setExpiresAt(initialExpiresAt);
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

        // Seguridad 1: Prevenir IDOR - El usuario autenticado debe ser quien reservó los boletos o un administrador
        boolean isOwner = sampleTicket.getUserId() != null && sampleTicket.getUserId().equals(userId);
        boolean isAdmin = userRepository.findByRoleName("admin").stream()
                .anyMatch(u -> u.getId().equals(userId));

        if (!isOwner && !isAdmin) {
            log.warn("Intento de confirmación no autorizado: usuario autenticado={} usuario de la reserva={}",
                    userId, sampleTicket.getUserId());
            throw new ValidationException("No tienes autorización para confirmar esta reserva de boletos");
        }

        // Idempotencia: Si ya fueron confirmados y vendidos para este usuario, retornar sin duplicar conteos
        if (sampleTicket.getStatus() == RaffleTicket.Status.SOLD) {
            return tickets.stream()
                    .map(t -> RaffleTicketResponse.from(t, userId))
                    .collect(Collectors.toList());
        }

        // Seguridad 2: Verificar con Stripe / Pasarela que el cobro esté realmente liquidado/succeeded
        boolean paymentSuccessful = paymentService.isPaymentSuccessful(paymentIntentId);
        if (!paymentSuccessful) {
            log.warn("Confirmación rechazada: el pago {} no está liquidado en la pasarela de pagos", paymentIntentId);
            throw new ValidationException("El pago no ha sido acreditado por Stripe o se encuentra pendiente de acreditación");
        }

        Raffle raffle = raffleRepository.findById(sampleTicket.getRaffleId())
                .orElseThrow(() -> new NotFoundException("Rifa"));

        String buyerId = sampleTicket.getUserId() != null ? sampleTicket.getUserId() : userId;
        LocalDateTime now = LocalDateTime.now();
        for (RaffleTicket ticket : tickets) {
            ticket.setStatus(RaffleTicket.Status.SOLD);
            ticket.setUserId(buyerId);
            ticket.setPurchasedAt(now);
        }
        ticketRepository.saveAll(tickets);

        // Actualizar ticketsSold en la entidad Raffle
        long totalSold = ticketRepository.countByRaffleIdAndStatus(raffle.getId(), RaffleTicket.Status.SOLD);
        raffle.setTicketsSold((int) totalSold);

        // Si se vendieron todos los boletos, pasa a DRAW y notifica a los administradores
        if (raffle.getTicketsSold() >= raffle.getTicketCount()) {
            boolean wasNotDraw = raffle.getStatus() != Raffle.Status.DRAW;
            raffle.setStatus(Raffle.Status.DRAW);
            log.info("Rifa {} vendió todos sus boletos y avanza a DRAW", raffle.getName());

            if (wasNotDraw) {
                notifyAdminsTicketsSoldOut(raffle);
            }
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

    /**
     * Registra que el usuario eligió pagar vía OXXO y extiende la reserva de los boletos a 3 horas.
     */
    @Transactional
    public OxxoReservationResponse registerOxxoVoucher(String paymentIntentId, String userId) {
        List<RaffleTicket> tickets = ticketRepository.findByPaymentIntentId(paymentIntentId);
        if (tickets.isEmpty()) {
            throw new NotFoundException("No se encontraron boletos asociados a la orden " + paymentIntentId);
        }

        RaffleTicket sampleTicket = tickets.get(0);
        if (sampleTicket.getUserId() == null || !sampleTicket.getUserId().equals(userId)) {
            throw new ValidationException("No tienes autorización para gestionar esta reserva de boletos");
        }

        if (sampleTicket.getStatus() == RaffleTicket.Status.SOLD) {
            throw new ValidationException("Estos boletos ya han sido comprados");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(OXXO_RESERVATION_EXPIRATION_HOURS);

        for (RaffleTicket ticket : tickets) {
            ticket.setPaymentMethod("OXXO");
            ticket.setExpiresAt(expiresAt);
        }
        ticketRepository.saveAll(tickets);

        log.info("Reserva OXXO extendida a 3 horas: pi={} usuario={} expiraEn={}",
                paymentIntentId, userId, expiresAt);

        return new OxxoReservationResponse(
                paymentIntentId,
                "OXXO",
                expiresAt,
                OXXO_RESERVATION_EXPIRATION_HOURS * 60L,
                tickets.stream().map(RaffleTicket::getTicketNumber).sorted().toList()
        );
    }

    /**
     * Simula la acreditación del pago en efectivo en OXXO (Modo prueba / evaluación académica).
     * Marca el pago como COMPLETED y confirma la compra a SOLD, actualizando ganancias y notificando si se agota.
     */
    @Transactional
    public List<RaffleTicketResponse> simulateOxxoPayment(String paymentIntentId,
                                                          String userId,
                                                          HttpServletRequest httpRequest) {
        List<RaffleTicket> tickets = ticketRepository.findByPaymentIntentId(paymentIntentId);
        if (tickets.isEmpty()) {
            throw new NotFoundException("No se encontraron boletos asociados al pago " + paymentIntentId);
        }

        RaffleTicket sampleTicket = tickets.get(0);
        boolean isOwner = sampleTicket.getUserId() != null && sampleTicket.getUserId().equals(userId);
        boolean isAdmin = userRepository.findByRoleName("admin").stream()
                .anyMatch(u -> u.getId().equals(userId));

        if (!isOwner && !isAdmin) {
            throw new ValidationException("Solo el administrador o el titular de la reserva pueden acreditar este pago");
        }

        if (sampleTicket.getStatus() == RaffleTicket.Status.SOLD) {
            return tickets.stream()
                    .map(t -> RaffleTicketResponse.from(t, userId))
                    .collect(Collectors.toList());
        }

        // Marcar pago localmente como COMPLETED para simulación de prueba
        paymentService.simulatePaymentSuccess(paymentIntentId);

        // Confirmar compra estándar
        return confirmPurchase(paymentIntentId, userId, httpRequest);
    }

    /**
     * Lista todas las reservas OXXO / en efectivo pendientes de pago para el panel de administración.
     */
    @Transactional
    public List<PendingOxxoReservationDto> getPendingOxxoReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<RaffleTicket> reservedTickets = ticketRepository.findByStatus(RaffleTicket.Status.RESERVED);

        // Limpiar reservas expiradas
        boolean modified = false;
        List<RaffleTicket> activeTickets = new ArrayList<>();
        for (RaffleTicket t : reservedTickets) {
            if (isTicketReservationExpired(t, now)) {
                t.setStatus(RaffleTicket.Status.AVAILABLE);
                t.setPaymentIntentId(null);
                t.setUserId(null);
                t.setReservedAt(null);
                t.setExpiresAt(null);
                t.setPaymentMethod(null);
                modified = true;
            } else if ("OXXO".equalsIgnoreCase(t.getPaymentMethod()) && t.getPaymentIntentId() != null) {
                activeTickets.add(t);
            }
        }
        if (modified) {
            ticketRepository.saveAll(reservedTickets);
        }

        // Agrupar por paymentIntentId
        Map<String, List<RaffleTicket>> groupedByPaymentIntent = activeTickets.stream()
                .collect(Collectors.groupingBy(RaffleTicket::getPaymentIntentId));

        List<PendingOxxoReservationDto> result = new ArrayList<>();

        for (Map.Entry<String, List<RaffleTicket>> entry : groupedByPaymentIntent.entrySet()) {
            String piId = entry.getKey();
            List<RaffleTicket> group = entry.getValue();
            if (group.isEmpty()) continue;

            RaffleTicket sample = group.get(0);
            Raffle raffle = raffleRepository.findById(sample.getRaffleId()).orElse(null);
            if (raffle == null) continue;

            User user = sample.getUserId() != null
                    ? userRepository.findById(sample.getUserId()).orElse(null)
                    : null;

            List<Integer> ticketNumbers = group.stream()
                    .map(RaffleTicket::getTicketNumber)
                    .sorted()
                    .toList();

            BigDecimal ticketPrice = raffle.getTicketPrice() != null ? raffle.getTicketPrice() : BigDecimal.ZERO;
            BigDecimal totalAmount = ticketPrice.multiply(BigDecimal.valueOf(group.size()));

            LocalDateTime expiresAt = sample.getExpiresAt() != null
                    ? sample.getExpiresAt()
                    : (sample.getReservedAt() != null ? sample.getReservedAt().plusHours(OXXO_RESERVATION_EXPIRATION_HOURS) : now);

            long minutesRemaining = java.time.Duration.between(now, expiresAt).toMinutes();
            boolean isExpired = minutesRemaining <= 0;

            result.add(new PendingOxxoReservationDto(
                    piId,
                    raffle.getId(),
                    raffle.getName(),
                    user != null ? user.getId() : sample.getUserId(),
                    user != null ? user.getName() : "Usuario desconocido",
                    user != null ? user.getEmail() : "Sin correo",
                    ticketNumbers,
                    totalAmount,
                    sample.getReservedAt(),
                    expiresAt,
                    Math.max(0, minutesRemaining),
                    isExpired
            ));
        }

        result.sort((a, b) -> {
            if (a.reservedAt() == null || b.reservedAt() == null) return 0;
            return b.reservedAt().compareTo(a.reservedAt());
        });

        return result;
    }

    /**
     * Notifica a todos los administradores del sistema cuando una rifa vende el 100% de sus boletos.
     */
    private void notifyAdminsTicketsSoldOut(Raffle raffle) {
        try {
            List<User> admins = userRepository.findByRoleName("admin");
            String title = "¡Boletos agotados! — " + raffle.getName();
            String body = String.format(
                    "¡Éxito total! Se han vendido todos los boletos (%d de %d) de la rifa \"%s\". La rifa ya está lista para que realices el sorteo y selecciones al ganador.",
                    raffle.getTicketCount(), raffle.getTicketCount(), raffle.getName()
            );

            BigDecimal totalRevenue = raffle.getTicketPrice() != null
                    ? raffle.getTicketPrice().multiply(BigDecimal.valueOf(raffle.getTicketCount()))
                    : BigDecimal.ZERO;

            for (User admin : admins) {
                inAppNotificationService.create(
                        admin.getId(),
                        "SYSTEM",
                        title,
                        body,
                        "RAFFLE",
                        raffle.getId(),
                        "/admin/rifas?id=" + raffle.getId()
                );

                if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                    notificationService.sendRaffleSoldOutEmail(
                            admin.getEmail(),
                            admin.getName(),
                            raffle.getName(),
                            raffle.getTicketCount(),
                            String.format("$%,.2f MXN", totalRevenue),
                            raffle.getId()
                    );
                }
            }
            log.info("Notificación de boletos agotados enviada a {} administradores para la rifa {}",
                    admins.size(), raffle.getName());
        } catch (Exception e) {
            log.error("Error al notificar a los administradores sobre rifa agotada {}: {}",
                    raffle.getId(), e.getMessage());
        }
    }
}
