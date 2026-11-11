package com.semillatecnologica.backend.modules.raffles.controller;

import com.semillatecnologica.backend.modules.raffles.dto.*;
import com.semillatecnologica.backend.modules.raffles.service.RaffleTicketService;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Boletos y Checkout", description = "Selección de boletos y pagos con Stripe")
public class RaffleCheckoutController {

    private final RaffleTicketService ticketService;

    /**
     * Consulta los boletos de una rifa (disponibles, reservados, vendidos).
     * Endpoint público para la tienda.
     */
    @GetMapping("/raffles/{id}/tickets")
    @Operation(summary = "Consultar boletos de una rifa",
               description = "Público — Lista de boletos del 1 al N con su estado.")
    public ResponseEntity<ApiResponse<List<RaffleTicketResponse>>> getTickets(
            @PathVariable String id,
            Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        List<RaffleTicketResponse> tickets = ticketService.getTicketsForRaffle(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Boletos de la rifa", tickets));
    }

    /**
     * Crea un intento de pago en Stripe (PaymentIntent) y aparta temporalmente los boletos.
     */
    @PostMapping("/checkout/crear-intento")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear intento de checkout",
               description = "Requiere autenticación. Aparta los boletos y devuelve clientSecret de Stripe.")
    public ResponseEntity<ApiResponse<CheckoutIntentResponse>> createIntent(
            @Valid @RequestBody CreateCheckoutIntentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        CheckoutIntentResponse response = ticketService.createCheckoutIntent(
                request, authentication.getName(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Intento de checkout generado", response));
    }

    /**
     * Confirma la compra tras el éxito en Stripe Elements.
     */
    @PostMapping("/checkout/confirmar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirmar compra de boletos",
               description = "Requiere autenticación. Marca los boletos como SOLD.")
    public ResponseEntity<ApiResponse<List<RaffleTicketResponse>>> confirmCheckout(
            @Valid @RequestBody ConfirmCheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        List<RaffleTicketResponse> confirmedTickets = ticketService.confirmPurchase(
                request.paymentIntentId(), authentication.getName(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Compra confirmada exitosamente", confirmedTickets));
    }

    /**
     * Registra que el usuario seleccionó pagar vía OXXO y extiende la reserva de boletos a 3 horas.
     */
    @PostMapping("/checkout/registrar-oxxo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar reserva OXXO (3 horas)",
               description = "Requiere autenticación. Extiende la reserva a 3 horas tras generar el voucher de OXXO.")
    public ResponseEntity<ApiResponse<OxxoReservationResponse>> registerOxxo(
            @Valid @RequestBody RegisterOxxoRequest request,
            Authentication authentication) {
        OxxoReservationResponse response = ticketService.registerOxxoVoucher(
                request.paymentIntentId(), authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Reserva OXXO registrada (3 horas)", response));
    }

    /**
     * Simula la acreditación del pago en OXXO (Herramienta para modo de prueba / evaluación docente).
     */
    @PostMapping("/checkout/simular-oxxo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Simular pago en OXXO (Modo prueba)",
               description = "Requiere autenticación. Simula la acreditación en efectivo de OXXO y confirma la compra.")
    public ResponseEntity<ApiResponse<List<RaffleTicketResponse>>> simulateOxxo(
            @Valid @RequestBody SimulateOxxoRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        List<RaffleTicketResponse> confirmedTickets = ticketService.simulateOxxoPayment(
                request.paymentIntentId(), authentication.getName(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Pago en OXXO simulado y acreditado con éxito", confirmedTickets));
    }

    /**
     * Consulta las reservas OXXO / en efectivo pendientes de pago (Vista de Administrador).
     */
    @GetMapping("/admin/checkout/reservas-oxxo")
    @PreAuthorize("hasAuthority('raffles.read') or hasAuthority('raffles.write')")
    @Operation(summary = "Consultar reservas OXXO pendientes",
               description = "Requiere permiso de rifas. Devuelve todas las reservas OXXO en espera de pago.")
    public ResponseEntity<ApiResponse<List<PendingOxxoReservationDto>>> getPendingOxxoReservations() {
        List<PendingOxxoReservationDto> reservations = ticketService.getPendingOxxoReservations();
        return ResponseEntity.ok(ApiResponse.ok("Reservas OXXO pendientes", reservations));
    }
}
