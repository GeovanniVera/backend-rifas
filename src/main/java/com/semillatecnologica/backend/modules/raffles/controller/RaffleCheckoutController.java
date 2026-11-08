package com.semillatecnologica.backend.modules.raffles.controller;

import com.semillatecnologica.backend.modules.raffles.dto.CheckoutIntentResponse;
import com.semillatecnologica.backend.modules.raffles.dto.ConfirmCheckoutRequest;
import com.semillatecnologica.backend.modules.raffles.dto.CreateCheckoutIntentRequest;
import com.semillatecnologica.backend.modules.raffles.dto.RaffleTicketResponse;
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
}
