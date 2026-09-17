package com.semillatecnologica.backend.modules.payments.strategy;

import java.util.Map;

/**
 * Evento de pago verificado desde un webhook.
 *
 * <p>Representa un evento externo que ya fue verificado
 * contra la firma del gateway.</p>
 *
 * @param gatewayId ID del evento en el gateway
 * @param paymentId ID del pago asociado
 * @param eventType Tipo de evento (payment.completed, payment.failed, etc.)
 * @param status Nuevo estado del pago
 * @param amount Monto en centavos
 * @param currency Moneda
 * @param metadata Metadata adicional del evento
 */
public record VerifiedPaymentEvent(
    String gatewayId,
    String paymentId,
    String eventType,
    String status,
    long amount,
    String currency,
    Map<String, String> metadata
) {}
