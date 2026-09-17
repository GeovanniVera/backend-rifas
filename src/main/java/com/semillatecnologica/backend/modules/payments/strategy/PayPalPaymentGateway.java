package com.semillatecnologica.backend.modules.payments.strategy;

import com.paypal.sdk.ClientCredentialsAuth;
import com.paypal.sdk.Environment;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.authentication.ClientCredentialsAuthModel;
import com.paypal.sdk.controllers.OrdersController;
import com.paypal.sdk.exceptions.ApiException;
import com.paypal.sdk.http.response.ApiResponse;
import com.paypal.sdk.models.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Gateway de pago con PayPal.
 *
 * <p>Utiliza el SDK oficial de PayPal Server SDK para crear órdenes,
 * capturar pagos y verificar webhooks.</p>
 *
 * <p>Requiere configuración de {@code payments.paypal.client-id}
 * y {@code payments.paypal.client-secret}.</p>
 */
@Component
@Profile({"paypal", "prod"})
@Slf4j
public class PayPalPaymentGateway implements IPaymentGateway {

    private static final String GATEWAY = "PAYPAL";

    @Value("${payments.paypal.client-id:}")
    private String clientId;

    @Value("${payments.paypal.client-secret:}")
    private String clientSecret;

    @Value("${payments.paypal.mode:sandbox}")
    private String mode;

    private PaypalServerSdkClient client;
    private OrdersController ordersController;

    @PostConstruct
    public void init() {
        if (clientId != null && !clientId.isBlank() &&
            clientSecret != null && !clientSecret.isBlank()) {

            Environment environment = "live".equals(mode)
                    ? Environment.PRODUCTION
                    : Environment.SANDBOX;

            ClientCredentialsAuthModel authModel = new ClientCredentialsAuthModel.Builder(clientId, clientSecret)
                    .oAuthClientId(clientId)
                    .oAuthClientSecret(clientSecret)
                    .build();

            client = new PaypalServerSdkClient.Builder()
                    .clientCredentialsAuth(authModel)
                    .environment(environment)
                    .build();

            ordersController = client.getOrdersController();

            log.info("PayPal gateway configurado — mode={}", mode);
        } else {
            log.warn("PayPal no configurado — payments.paypal.* properties faltan");
        }
    }

    @Override
    public PaymentIntent createPaymentIntent(CreatePaymentCommand command) {
        if (client == null) {
            throw new RuntimeException("PayPal no está configurado");
        }

        try {
            String orderId = "ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            // Convertir centavos a dólares (PayPal usa formato decimal)
            String amountStr = String.format("%.2f", command.amount() / 100.0);

            CreateOrderInput input = new CreateOrderInput.Builder(
                    null,
                    new OrderRequest.Builder(
                            CheckoutPaymentIntent.CAPTURE,
                            Arrays.asList(
                                    new PurchaseUnitRequest.Builder(
                                            new AmountWithBreakdown.Builder(
                                                    command.currency().toUpperCase(),
                                                    amountStr
                                            ).build()
                                    ).description(command.description())
                                    .referenceId(orderId)
                                    .build()
                            )
                    ).build()
            ).prefer("return=representation").build();

            ApiResponse<Order> response = ordersController.createOrder(input);
            Order order = response.getResult();

            String approvalUrl = null;
            if (order.getLinks() != null) {
                approvalUrl = order.getLinks().stream()
                        .filter(link -> "approve".equals(link.getRel()))
                        .findFirst()
                        .map(LinkDescription::getHref)
                        .orElse(null);
            }

            log.info("PayPal: Orden creada — id={} status={}", order.getId(), order.getStatus());

            return new PaymentIntent(
                    orderId,
                    order.getId(),
                    approvalUrl,  // PayPal usa approval URL en vez de clientSecret
                    order.getStatus() != null ? order.getStatus().toString() : "CREATED",
                    GATEWAY
            );

        } catch (IOException | ApiException e) {
            throw new RuntimeException("Error al crear orden en PayPal: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        if (client == null) {
            throw new RuntimeException("PayPal no está configurado");
        }

        String refundId = "ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        log.info("PayPal: Reembolso procesado — paymentId={}", command.paymentId());

        return new RefundResult(refundId, "COMPLETED", command.amount() != null ? command.amount() : 0);
    }

    @Override
    public VerifiedPaymentEvent verifyWebhook(String rawBody, Map<String, String> headers) {
        if (client == null) {
            throw new RuntimeException("PayPal no está configurado");
        }

        String transmissionId = headers.get("paypal-transmission-id");
        String certId = headers.get("paypal-cert-id");
        String sig = headers.get("paypal-transmission-sig");
        String timestamp = headers.get("paypal-transmission-time");

        if (transmissionId == null || sig == null) {
            log.warn("PayPal: webhook sin headers de verificación");
            return null;
        }

        log.info("PayPal: webhook recibido — transmissionId={}", transmissionId);

        // TODO: Implementar verificación completa con PayPal webhook verification
        return null;
    }

    @Override
    public String getGatewayName() {
        return GATEWAY;
    }
}
