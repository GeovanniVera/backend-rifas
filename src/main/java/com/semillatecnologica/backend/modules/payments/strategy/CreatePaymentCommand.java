package com.semillatecnologica.backend.modules.payments.strategy;

import java.util.Map;

/**
 * Comando para crear un pago.
 *
 * @param amount Monto en centavos (ej: 1000 = $10.00)
 * @param currency Código de moneda (USD, EUR, ARS)
 * @param description Descripción del pago
 * @param userId ID del usuario que paga
 * @param metadata Metadata adicional del pago
 */
public record CreatePaymentCommand(
    long amount,
    String currency,
    String description,
    String userId,
    Map<String, String> metadata
) {}
