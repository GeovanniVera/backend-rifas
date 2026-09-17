package com.semillatecnologica.backend.modules.payments.factory;

import com.semillatecnologica.backend.modules.payments.strategy.IPaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fábrica que resuelve el gateway de pago según configuración.
 *
 * <p>Selecta entre InMemory, Stripe y PayPal según la propiedad
 * {@code payments.gateway} en application.yaml.</p>
 *
 * <p>Inyecta la lista de beans activos del profile actual.
 * Si un gateway no tiene profile activo, simplemente no se registra.</p>
 */
@Component
@Slf4j
public class PaymentGatewayFactory {

    private final Map<String, IPaymentGateway> gateways;

    @Value("${payments.gateway:IN_MEMORY}")
    private String configuredGateway;

    public PaymentGatewayFactory(List<IPaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
                .collect(Collectors.toMap(
                        IPaymentGateway::getGatewayName,
                        Function.identity()
                ));
        log.info("Gateways de pago registrados: {}", gateways.keySet());
    }

    /**
     * Resuelve el gateway configurado para el entorno actual.
     *
     * @return Optional con el gateway si existe y está configurado
     */
    public Optional<IPaymentGateway> resolve() {
        return resolve(configuredGateway);
    }

    /**
     * Resuelve un gateway por nombre.
     *
     * @param gatewayName Nombre del gateway (IN_MEMORY, STRIPE, PAYPAL)
     * @return Optional con el gateway si existe y está configurado
     */
    public Optional<IPaymentGateway> resolve(String gatewayName) {
        IPaymentGateway gateway = gateways.get(gatewayName.toUpperCase());
        if (gateway == null) {
            log.warn("Gateway de pago no encontrado: {}", gatewayName);
            return Optional.empty();
        }
        return Optional.of(gateway);
    }

    /**
     * Retorna el nombre del gateway configurado actualmente.
     */
    public String getConfiguredGateway() {
        return configuredGateway;
    }
}
