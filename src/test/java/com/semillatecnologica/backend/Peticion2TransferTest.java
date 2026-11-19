package com.semillatecnologica.backend;

import com.semillatecnologica.backend.modules.admin.service.AuditService;
import com.semillatecnologica.backend.modules.auth.repository.UserRepository;
import com.semillatecnologica.backend.modules.notification.inapp.service.InAppNotificationService;
import com.semillatecnologica.backend.modules.notification.service.NotificationService;
import com.semillatecnologica.backend.modules.payments.service.PaymentService;
import com.semillatecnologica.backend.modules.raffles.model.Raffle;
import com.semillatecnologica.backend.modules.raffles.model.RaffleTicket;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleRepository;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleTicketRepository;
import com.semillatecnologica.backend.modules.raffles.service.RaffleTicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Peticion2TransferTest {

    @Mock private RaffleRepository raffleRepository;
    @Mock private RaffleTicketRepository ticketRepository;
    @Mock private PaymentService paymentService;
    @Mock private AuditService auditService;
    @Mock private InAppNotificationService inAppNotificationService;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;

    @Test
    void registerTransferSetsMethodTransferAndThreeHourExpiration() {
        RaffleTicketService service = new RaffleTicketService(
                raffleRepository, ticketRepository, paymentService,
                auditService, inAppNotificationService, notificationService, userRepository
        );

        Raffle raffle = Raffle.builder()
                .id("r-1")
                .productId("p-1")
                .name("Rifa Test")
                .ticketCount(29)
                .ticketsSold(0)
                .ticketPrice(new BigDecimal("350"))
                .marginPercent(new BigDecimal("100.00"))
                .status(Raffle.Status.ACTIVE)
                .build();

        RaffleTicket t1 = RaffleTicket.builder()
                .id("t-1")
                .raffleId("r-1")
                .ticketNumber(4)
                .userId("user-1")
                .paymentIntentId("pi_test_123abc")
                .status(RaffleTicket.Status.RESERVED)
                .reservedAt(LocalDateTime.now())
                .build();

        when(ticketRepository.findByPaymentIntentId("pi_test_123abc")).thenReturn(List.of(t1));
        when(raffleRepository.findById("r-1")).thenReturn(Optional.of(raffle));

        var res = service.registerTransferReservation("pi_test_123abc", "RASTREO-999", "user-1");

        assertThat(res.paymentMethod()).isEqualTo("TRANSFER");
        assertThat(res.referenceCode()).isEqualTo("TRF-123ABC");
        assertThat(res.trackingReference()).isEqualTo("RASTREO-999");
        assertThat(res.totalAmount()).isEqualByComparingTo("350");
        assertThat(res.expiresInMinutes()).isEqualTo(180);
        assertThat(t1.getPaymentMethod()).isEqualTo("TRANSFER");
        verify(paymentService).attachTransferMetadata("pi_test_123abc", "TRF-123ABC", "RASTREO-999");
    }

    @Test
    void confirmTransferPaymentMarksTicketsSold() {
        RaffleTicketService service = new RaffleTicketService(
                raffleRepository, ticketRepository, paymentService,
                auditService, inAppNotificationService, notificationService, userRepository
        );

        Raffle raffle = Raffle.builder()
                .id("r-1")
                .productId("p-1")
                .name("Rifa Test")
                .ticketCount(29)
                .ticketsSold(0)
                .ticketPrice(new BigDecimal("350"))
                .marginPercent(new BigDecimal("100.00"))
                .status(Raffle.Status.ACTIVE)
                .build();

        RaffleTicket t1 = RaffleTicket.builder()
                .id("t-1")
                .raffleId("r-1")
                .ticketNumber(4)
                .userId("user-1")
                .paymentIntentId("pi_test_123abc")
                .paymentMethod("TRANSFER")
                .status(RaffleTicket.Status.RESERVED)
                .reservedAt(LocalDateTime.now())
                .build();

        var adminUser = com.semillatecnologica.backend.modules.auth.model.User.builder()
                .id("admin-1")
                .email("admin@test.com")
                .name("Admin")
                .password("x")
                .build();

        when(ticketRepository.findByPaymentIntentId("pi_test_123abc")).thenReturn(List.of(t1));
        when(userRepository.findByRoleName("admin")).thenReturn(List.of(adminUser));
        when(paymentService.isPaymentSuccessful("pi_test_123abc")).thenReturn(true);
        when(raffleRepository.findById("r-1")).thenReturn(Optional.of(raffle));
        when(ticketRepository.countByRaffleIdAndStatus("r-1", RaffleTicket.Status.SOLD)).thenReturn(1L);

        var result = service.confirmTransferPayment("pi_test_123abc", "admin-1", null);

        assertThat(result).hasSize(1);
        assertThat(t1.getStatus()).isEqualTo(RaffleTicket.Status.SOLD);
        assertThat(t1.getPaymentMethod()).isEqualTo("TRANSFER");
        assertThat(raffle.getTicketsSold()).isEqualTo(1);
        verify(paymentService).simulatePaymentSuccess("pi_test_123abc");
    }
}
