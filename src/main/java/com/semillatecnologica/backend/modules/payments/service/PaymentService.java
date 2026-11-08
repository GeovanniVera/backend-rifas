package com.semillatecnologica.backend.modules.payments.service;

import com.semillatecnologica.backend.modules.payments.factory.PaymentGatewayFactory;
import com.semillatecnologica.backend.modules.payments.model.Payment;
import com.semillatecnologica.backend.modules.payments.repository.PaymentRepository;
import com.semillatecnologica.backend.modules.payments.strategy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio orquestador de pagos.
 *
 * <p>Flujo:
 * <pre>
 * PaymentService.createPayment(command)
 *   → generar idempotencyKey
 *   → PaymentGatewayFactory.resolve()
 *   → IPaymentGateway.createPaymentIntent(command)
 *   → PaymentRepository.save(...)
 *   → PaymentIntent (con clientSecret para frontend)
 * </pre>
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Generar claves de idempotencia</li>
 *   <li>Persistir estado local del pago</li>
 *   <li>Delegar al gateway configurado</li>
 *   <li>Manejar reembolsos y actualizaciones de estado</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentRepository paymentRepository;

    /**
     * Crea un pago y retorna el PaymentIntent para el frontend.
     *
     * @param userId ID del usuario que paga
     * @param amount Monto en centavos
     * @param currency Moneda (USD, EUR, ARS)
     * @param description Descripción del pago
     * @param metadata Metadata adicional
     * @return PaymentIntent con clientSecret para el frontend
     */
    @Transactional
    public PaymentIntent createPayment(String userId, long amount, String currency,
                                        String description, Map<String, String> metadata) {
        // 1. Delegar al gateway configurado
        IPaymentGateway gateway = gatewayFactory.resolve()
                .orElseThrow(() -> new RuntimeException("No hay gateway de pago configurado"));

        // 2. Generar idempotency key única
        String idempotencyKey = UUID.randomUUID().toString();

        CreatePaymentCommand command = new CreatePaymentCommand(
                amount, currency, description, userId, metadata
        );

        PaymentIntent intent = gateway.createPaymentIntent(command);

        // 3. Crear y persistir el pago local con gateway asignado (evita violación de NOT NULL)
        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .gateway(intent.gateway())
                .gatewayId(intent.gatewayId())
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .currency(currency)
                .status(intent.status())
                .description(description)
                .metadataJson(metadata)
                .paidAt("COMPLETED".equals(intent.status()) ? LocalDateTime.now() : null)
                .build();

        payment = paymentRepository.save(payment);

        log.info("Pago creado: userId={} amount={} {} gateway={} status={}",
                userId, amount, currency, intent.gateway(), intent.status());

        return new PaymentIntent(
                payment.getId(),
                intent.gatewayId(),
                intent.clientSecret(),
                intent.status(),
                intent.gateway()
        );
    }

    /**
     * Procesa un reembolso.
     *
     * @param paymentId ID del pago a reembolsar
     * @param amount Monto a reembolsar (null = completo)
     * @param reason Razón del reembolso
     * @return Resultado del reembolso
     */
    @Transactional
    public RefundResult refund(String paymentId, Long amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));

        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new RuntimeException("Solo se pueden reembolsar pagos completados");
        }

        IPaymentGateway gateway = gatewayFactory.resolve()
                .orElseThrow(() -> new RuntimeException("No hay gateway de pago configurado"));

        RefundCommand command = new RefundCommand(payment.getGatewayId(), amount, reason);
        RefundResult result = gateway.refund(command);

        // Actualizar estado local
        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);

        log.info("Reembolso procesado: paymentId={} refundId={}", paymentId, result.refundId());
        return result;
    }

    /**
     * Procesa un evento verificado desde un webhook.
     *
     * @param event Evento verificado por el gateway
     */
    @Transactional
    public void handleWebhookEvent(VerifiedPaymentEvent event) {
        if (event == null) return;

        Payment payment = paymentRepository.findByGatewayId(event.paymentId())
                .orElse(null);

        if (payment == null) {
            log.warn("Webhook para pago no encontrado: gatewayId={}", event.paymentId());
            return;
        }

        payment.setStatus(event.status());
        if ("COMPLETED".equals(event.status())) {
            payment.setPaidAt(LocalDateTime.now());
        } else if ("FAILED".equals(event.status())) {
            payment.setFailureReason("Payment failed via webhook");
        }

        paymentRepository.save(payment);

        log.info("Estado actualizado por webhook: paymentId={} status={}", payment.getId(), event.status());
    }

    /**
     * Verifica si un pago fue completado con éxito en el gateway proveedor.
     * Si es exitoso, actualiza el estado local del pago a COMPLETED.
     *
     * @param gatewayId Identificador del pago en el gateway (ej: PaymentIntent ID de Stripe)
     * @return true si el pago está exitosamente acreditado
     */
    @Transactional
    public boolean isPaymentSuccessful(String gatewayId) {
        IPaymentGateway gateway = gatewayFactory.resolve()
                .orElseThrow(() -> new RuntimeException("No hay gateway de pago configurado"));

        boolean successful = gateway.isPaymentSuccessful(gatewayId);
        if (successful) {
            paymentRepository.findByGatewayId(gatewayId).ifPresent(payment -> {
                if (!"COMPLETED".equalsIgnoreCase(payment.getStatus())) {
                    payment.setStatus("COMPLETED");
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    log.info("Pago {} marcado como COMPLETED tras verificación del gateway", gatewayId);
                }
            });
        }
        return successful;
    }

    /**
     * Retorna un pago por ID.
     */
    @Transactional(readOnly = true)
    public Payment getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));
    }
}
