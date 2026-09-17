package com.semillatecnologica.backend.modules.raffles.repository;

import com.semillatecnologica.backend.modules.raffles.model.Raffle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de rifas.
 */
@Repository
public interface RaffleRepository extends JpaRepository<Raffle, String> {

    /**
     * Rifas activas (para la tienda pública).
     */
    List<Raffle> findByStatusOrderByCreatedAtDesc(Raffle.Status status);

    /**
     * Rifas de un producto.
     */
    List<Raffle> findByProductIdOrderByCreatedAtDesc(String productId);
}