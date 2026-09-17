package com.semillatecnologica.backend.modules.payments.webhook;

import com.semillatecnologica.backend.modules.payments.factory.PaymentGatewayFactory;
import com.semillatecnologica.backend.modules.payments.service.PaymentService;
import com.semillatecnologica.backend.modules.payments.strategy.IPaymentGateway;
import com.semillatecnologica.backend.modules.payments.strategy.VerifiedPaymentEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de webhooks de pago.
 *
 * <p>Recibe notificaciones de Stripe/PayPal cuando cambia
 * el estado de un pago. NO usa autenticación de sesión —
 * verifica la firma del webhook en su lugar.</p>
 *
 * <p>Endpoints públicos (sin auth de sesión):
 * <ul>
 *   <li>POST /webhooks/payments/stripe</li>
 *   <li>POST /webhooks/payments/paypal</li>
 * </ul>
 */
@RestController
@RequestMapping("/webhooks/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks de Pago", description = "Recepción de eventos de gateways de pago")
public class PaymentWebhookController {

    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentService paymentService;

    /**
     * Recibe webhooks de Stripe.
     *
     * @param rawBody Cuerpo raw del request
     * @param request HTTP request para headers
     * @return 200 OK si se procesó correctamente
     */
    @PostMapping("/stripe")
    @Operation(summary = "Webhook de Stripe",
               description = "Recibe eventos de pago de Stripe. Verifica firma automáticamente.")
    public ResponseEntity<Map<String, String>> handleStripeWebhook(
            @RequestBody String rawBody,
            HttpServletRequest request) {

        return processWebhook("STRIPE", rawBody, request);
    }

    /**
     * Recibe webhooks de PayPal.
     *
     * @param rawBody Cuerpo raw del request
     * @param request HTTP request para headers
     * @return 200 OK si se procesó correctamente
     */
    @PostMapping("/paypal")
    @Operation(summary = "Webhook de PayPal",
               description = "Recibe eventos de pago de PayPal. Verifica firma automáticamente.")
    public ResponseEntity<Map<String, String>> handlePayPalWebhook(
            @RequestBody String rawBody,
            HttpServletRequest request) {

        return processWebhook("PAYPAL", rawBody, request);
    }

    /**
     * Procesa un webhook genérico.
     */
    private ResponseEntity<Map<String, String>> processWebhook(
            String gatewayName, String rawBody, HttpServletRequest request) {

        log.info("Webhook recibido de {}", gatewayName);

        // 1. Extraer headers relevantes
        Map<String, String> headers = extractHeaders(request, gatewayName);

        // 2. Resolver el gateway
        IPaymentGateway gateway = gatewayFactory.resolve(gatewayName).orElse(null);
        if (gateway == null) {
            log.warn("Gateway no disponible: {}", gatewayName);
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "gateway_not_configured"));
        }

        // 3. Verificar firma y parsear evento
        VerifiedPaymentEvent event = gateway.verifyWebhook(rawBody, headers);

        if (event == null) {
            log.warn("Webhook inválido o no verificado de {}", gatewayName);
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "invalid_signature"));
        }

        // 4. Procesar el evento
        paymentService.handleWebhookEvent(event);

        log.info("Webhook procesado: gateway={} eventType={} status={}",
                gatewayName, event.eventType(), event.status());

        return ResponseEntity.ok(Map.of("status", "processed"));
    }

    /**
     * Extrae los headers relevantes para cada gateway.
     */
    private Map<String, String> extractHeaders(HttpServletRequest request, String gatewayName) {
        Map<String, String> headers = new HashMap<>();

        switch (gatewayName) {
            case "STRIPE" -> {
                headers.put("stripe-signature", request.getHeader("stripe-signature"));
            }
            case "PAYPAL" -> {
                headers.put("paypal-transmission-id", request.getHeader("paypal-transmission-id"));
                headers.put("paypal-cert-id", request.getHeader("paypal-cert-id"));
                headers.put("paypal-transmission-sig", request.getHeader("paypal-transmission-sig"));
                headers.put("paypal-transmission-time", request.getHeader("paypal-transmission-time"));
            }
        }

        return headers;
    }
}
