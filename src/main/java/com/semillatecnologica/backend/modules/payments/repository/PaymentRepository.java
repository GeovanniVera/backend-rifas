package com.semillatecnologica.backend.modules.payments.repository;

import com.semillatecnologica.backend.modules.payments.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de pagos.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * Busca un pago por su ID de idempotencia.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Busca un pago por su ID en el gateway externo.
     */
    Optional<Payment> findByGatewayId(String gatewayId);
}
