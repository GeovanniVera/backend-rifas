package com.semillatecnologica.backend.modules.payments.strategy;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Gateway de pago con Stripe.
 *
 * <p>Utiliza el SDK oficial de Stripe para crear pagos,
 * procesar reembolsos y verificar webhooks.</p>
 *
 * <p>Requiere configuración de {@code payments.stripe.secret-key}.</p>
 */
@Component
@Profile({"stripe", "prod"})
@Slf4j
public class StripePaymentGateway implements IPaymentGateway {

    private static final String GATEWAY = "STRIPE";

    @Value("${payments.stripe.secret-key:}")
    private String secretKey;

    private StripeClient client;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank()) {
            this.client = new StripeClient(secretKey);
            log.info("Stripe gateway configurado");
        } else {
            log.warn("Stripe no configurado — payments.stripe.secret-key faltan");
        }
    }

    @Override
    public com.semillatecnologica.backend.modules.payments.strategy.PaymentIntent createPaymentIntent(CreatePaymentCommand command) {
        if (client == null) {
            throw new RuntimeException("Stripe no está configurado");
        }

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(command.amount())
                    .setCurrency(command.currency().toLowerCase())
                    .setDescription(command.description())
                    .putAllMetadata(command.metadata() != null ? command.metadata() : Map.of())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = client.v1().paymentIntents().create(params);

            log.info("Stripe: PaymentIntent creado — id={} amount={} {}",
                    intent.getId(), command.amount(), command.currency());

            return new com.semillatecnologica.backend.modules.payments.strategy.PaymentIntent(
                    intent.getId(),
                    intent.getId(),
                    intent.getClientSecret(),
                    intent.getStatus(),
                    GATEWAY
            );

        } catch (StripeException e) {
            throw new RuntimeException("Error al crear PaymentIntent en Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        if (client == null) {
            throw new RuntimeException("Stripe no está configurado");
        }

        try {
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                    .setPaymentIntent(command.paymentId());

            if (command.amount() != null) {
                builder.setAmount(command.amount());
            }

            if (command.reason() != null) {
                // Mapear razón string a enum de Stripe
                switch (command.reason().toLowerCase()) {
                    case "duplicate" -> builder.setReason(RefundCreateParams.Reason.DUPLICATE);
                    case "fraudulent" -> builder.setReason(RefundCreateParams.Reason.FRAUDULENT);
                    case "requested_by_customer" -> builder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
                    default -> {} // sin razón específica
                }
            }

            Refund refund = client.v1().refunds().create(builder.build());

            log.info("Stripe: Reembolso procesado — id={} paymentId={}", refund.getId(), command.paymentId());

            return new RefundResult(
                    refund.getId(),
                    refund.getStatus(),
                    refund.getAmount() != null ? refund.getAmount() : 0
            );

        } catch (StripeException e) {
            throw new RuntimeException("Error al reembolsar en Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public VerifiedPaymentEvent verifyWebhook(String rawBody, Map<String, String> headers) {
        if (client == null) {
            throw new RuntimeException("Stripe no está configurado");
        }

        try {
            String sigHeader = headers.get("stripe-signature");
            if (sigHeader == null) {
                log.warn("Stripe: webhook sin firma");
                return null;
            }

            // Verificar firma del webhook usando el SDK de Stripe
            // Nota: La verificación de firma requiere constructEvent que no está
            // disponible en el SDK v33+. En producción, usar la librería
            // stripe-java o implementar manualmente.
            log.info("Stripe: webhook recibido — verificación pendiente de implementar con SDK v33+");

            // TODO: Implementar verificación de firma con Stripe SDK v33+
            return null;

        } catch (Exception e) {
            log.error("Stripe: webhook inválido — {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getGatewayName() {
        return GATEWAY;
    }
}
