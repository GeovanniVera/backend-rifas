package com.semillatecnologica.backend.modules.payments.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway de pago en memoria para testing.
 *
 * <p>Simula pagos sin llamar a ningún API externo.
 * Útil para desarrollo, tests automatizados y demos.</p>
 *
 * <p>Comportamiento:
 * <ul>
 *   <li>Todos los pagos se aprueban automáticamente</li>
 *   <li>Los reembolsos siempre exitosos</li>
 *   <li>Los webhookssimulan eventos de prueba</li>
 * </ul>
 *
 * <p>Activo solo con el profile {@code test} o {@code inmemory}.</p>
 */
@Component
@Profile({"dev", "test", "inmemory"})
@Slf4j
public class InMemoryPaymentGateway implements IPaymentGateway {

    private static final String GATEWAY = "IN_MEMORY";

    private final Map<String, PaymentIntent> payments = new ConcurrentHashMap<>();
    private final Map<String, RefundResult> refunds = new ConcurrentHashMap<>();

    @Override
    public PaymentIntent createPaymentIntent(CreatePaymentCommand command) {
        String paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String gatewayId = "gw_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        PaymentIntent intent = new PaymentIntent(
                paymentId,
                gatewayId,
                "inmem_secret_" + paymentId,
                "COMPLETED",
                GATEWAY
        );

        payments.put(paymentId, intent);
        log.info("InMemory: pago creado — id={} amount={} {} status=COMPLETED",
                paymentId, command.amount(), command.currency());

        return intent;
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        String refundId = "ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        RefundResult result = new RefundResult(refundId, "COMPLETED", command.amount() != null ? command.amount() : 0);
        refunds.put(refundId, result);

        log.info("InMemory: reembolso procesado — refundId={} paymentId={}", refundId, command.paymentId());
        return result;
    }

    @Override
    public VerifiedPaymentEvent verifyWebhook(String rawBody, Map<String, String> headers) {
        log.info("InMemory: webhook recibido — simulando evento de prueba");
        return new VerifiedPaymentEvent(
                "evt_inmem_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                "pay_test",
                "payment.completed",
                "COMPLETED",
                1000,
                "USD",
                Map.of("test", "true")
        );
    }

    @Override
    public String getGatewayName() {
        return GATEWAY;
    }
}
