package com.semillatecnologica.backend.modules.raffles.repository;

import com.semillatecnologica.backend.modules.raffles.model.RaffleTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RaffleTicketRepository extends JpaRepository<RaffleTicket, String> {

    List<RaffleTicket> findByRaffleIdOrderByTicketNumberAsc(String raffleId);

    List<RaffleTicket> findByRaffleIdAndTicketNumberIn(String raffleId, Collection<Integer> ticketNumbers);

    List<RaffleTicket> findByPaymentIntentId(String paymentIntentId);

    boolean existsByRaffleId(String raffleId);

    long countByRaffleIdAndStatus(String raffleId, RaffleTicket.Status status);
}
