package com.semillatecnologica.backend.modules.payments.strategy;

/**
 * Resultado de crear un PaymentIntent.
 *
 * <p>Contiene la información que el frontend necesita para
 * confirmar el pago con el SDK del gateway.</p>
 *
 * @param id ID interno del pago
 * @param gatewayId ID del pago en el gateway externo
 * @param clientSecret Secret para el frontend (Stripe) o token (PayPal)
 * @param status Estado del intent
 * @param gateway Gateway que procesó el pago
 */
public record PaymentIntent(
    String id,
    String gatewayId,
    String clientSecret,
    String status,
    String gateway
) {}
