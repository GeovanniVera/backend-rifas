package com.semillatecnologica.backend.modules.payments.strategy;

/**
 * Resultado de un reembolso.
 *
 * @param refundId ID del reembolso en el gateway
 * @param status Estado del reembolso
 * @param amount Monto reembolsado en centavos
 */
public record RefundResult(
    String refundId,
    String status,
    long amount
) {}
