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
        // 1. Generar idempotency key única
        String idempotencyKey = UUID.randomUUID().toString();

        // 2. Verificar si ya existe un pago con esta key (protección contra retry)
        // (En producción, esto sería un índice único en la DB)

        // 3. Crear el pago local con estado PENDING
        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .currency(currency)
                .status("PENDING")
                .description(description)
                .metadataJson(metadata)
                .build();

        payment = paymentRepository.save(payment);

        // 4. Delegar al gateway configurado
        IPaymentGateway gateway = gatewayFactory.resolve()
                .orElseThrow(() -> new RuntimeException("No hay gateway de pago configurado"));

        CreatePaymentCommand command = new CreatePaymentCommand(
                amount, currency, description, userId, metadata
        );

        PaymentIntent intent = gateway.createPaymentIntent(command);

        // 5. Actualizar el pago local con la respuesta del gateway
        payment.setGatewayId(intent.gatewayId());
        payment.setGateway(intent.gateway());
        payment.setStatus(intent.status());

        if ("COMPLETED".equals(intent.status())) {
            payment.setPaidAt(LocalDateTime.now());
        }

        paymentRepository.save(payment);

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
     * Retorna un pago por ID.
     */
    @Transactional(readOnly = true)
    public Payment getPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + paymentId));
    }
}
