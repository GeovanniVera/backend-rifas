package com.semillatecnologica.backend.modules.payments.strategy;

/**
 * Interfaz Strategy para gateways de pago.
 *
 * <p>Cada implementación encapsula un proveedor de pagos.
 * El resto de la aplicación no conoce SDK, credenciales
 * ni protocolos del proveedor.</p>
 *
 * <p>Implementaciones:
 * <ul>
 *   <li>InMemoryPaymentGateway — testing sin llamadas externas</li>
 *   <li>StripePaymentGateway — Stripe API</li>
 *   <li>PayPalPaymentGateway — PayPal API</li>
 * </ul>
 */
public interface IPaymentGateway {

    /**
     * Crea un PaymentIntent para iniciar un pago.
     *
     * @param command Datos del pago
     * @return Intent con clientSecret para el frontend
     */
    PaymentIntent createPaymentIntent(CreatePaymentCommand command);

    /**
     * Procesa un reembolso.
     *
     * @param command Datos del reembolso
     * @return Resultado del reembolso
     */
    RefundResult refund(RefundCommand command);

    /**
     * Verifica y parsea un evento de webhook.
     *
     * <p>Retorna null si la firma es inválida o el evento
     * no es reconocido.</p>
     *
     * @param rawBody Cuerpo raw del webhook
     * @param headers Headers HTTP del webhook
     * @return Evento verificado, o null si es inválido
     */
    VerifiedPaymentEvent verifyWebhook(String rawBody, java.util.Map<String, String> headers);

    /**
     * Retorna el nombre del gateway.
     */
    String getGatewayName();
}
