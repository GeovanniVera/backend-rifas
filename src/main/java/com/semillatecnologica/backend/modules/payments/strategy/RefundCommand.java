package com.semillatecnologica.backend.modules.payments.strategy;

/**
 * Comando para procesar un reembolso.
 *
 * @param paymentId ID del pago a reembolsar
 * @param amount Monto a reembolsar en centavos (null = monto completo)
 * @param reason Razón del reembolso
 */
public record RefundCommand(
    String paymentId,
    Long amount,
    String reason
) {}
