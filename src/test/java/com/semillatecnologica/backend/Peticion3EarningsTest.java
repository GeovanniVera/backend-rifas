package com.semillatecnologica.backend;

import com.semillatecnologica.backend.modules.auth.model.User;
import com.semillatecnologica.backend.modules.auth.repository.UserRepository;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsPeriod;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsReportResponse;
import com.semillatecnologica.backend.modules.earnings.service.AdminEarningsService;
import com.semillatecnologica.backend.modules.earnings.service.EarningsReportExporter;
import com.semillatecnologica.backend.modules.raffles.model.Raffle;
import com.semillatecnologica.backend.modules.raffles.model.RaffleTicket;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleRepository;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleTicketRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Peticion3EarningsTest {

    @Mock private RaffleTicketRepository ticketRepository;
    @Mock private RaffleRepository raffleRepository;
    @Mock private UserRepository userRepository;

    @Test
    void earningsServiceCalculatesKpisAndExportGeneratesValidPdfAndXlsx() throws Exception {
        AdminEarningsService service = new AdminEarningsService(ticketRepository, raffleRepository, userRepository);
        EarningsReportExporter exporter = new EarningsReportExporter();

        Raffle raffle = Raffle.builder()
                .id("r-1")
                .productId("p-1")
                .name("Rifa — RTX 4070")
                .ticketCount(29)
                .ticketsSold(3)
                .ticketPrice(new BigDecimal("500"))
                .marginPercent(new BigDecimal("100.00"))
                .build();

        User buyer = User.builder()
                .id("u-1")
                .name("Carlos Pérez")
                .email("carlos@test.com")
                .password("x")
                .build();

        LocalDateTime now = LocalDateTime.now();
        RaffleTicket t1 = RaffleTicket.builder()
                .id("t-1")
                .raffleId("r-1")
                .ticketNumber(5)
                .userId("u-1")
                .paymentIntentId("pi_abc123")
                .paymentMethod("CARD")
                .status(RaffleTicket.Status.SOLD)
                .purchasedAt(now)
                .build();
        RaffleTicket t2 = RaffleTicket.builder()
                .id("t-2")
                .raffleId("r-1")
                .ticketNumber(8)
                .userId("u-1")
                .paymentIntentId("pi_abc123")
                .paymentMethod("CARD")
                .status(RaffleTicket.Status.SOLD)
                .purchasedAt(now)
                .build();
        RaffleTicket t3 = RaffleTicket.builder()
                .id("t-3")
                .raffleId("r-1")
                .ticketNumber(12)
                .userId("u-1")
                .paymentIntentId("pi_oxxo99")
                .paymentMethod("OXXO")
                .status(RaffleTicket.Status.SOLD)
                .purchasedAt(now)
                .build();

        when(ticketRepository.findByStatusAndPurchasedAtBetweenOrderByPurchasedAtDesc(
                eq(RaffleTicket.Status.SOLD), any(), any()))
                .thenReturn(List.of(t1, t2, t3))
                .thenReturn(List.of());

        when(raffleRepository.findAllById(any())).thenReturn(List.of(raffle));
        when(userRepository.findAllById(any())).thenReturn(List.of(buyer));

        EarningsReportResponse report = service.getEarnings(EarningsPeriod.FORTNIGHT, 0);

        assertThat(report.kpis().totalEarnings()).isEqualByComparingTo("1500");
        assertThat(report.kpis().ticketsSold()).isEqualTo(3);
        assertThat(report.kpis().transactionsCount()).isEqualTo(2);
        assertThat(report.kpis().averageTicket()).isEqualByComparingTo("750");
        assertThat(report.movements()).hasSize(2);
        assertThat(report.distribution()).extracting(EarningsReportResponse.DistributionSlice::category)
                .containsExactly("Tarjeta", "OXXO", "Transferencia");

        byte[] pdf = exporter.exportPdf(report, null);
        assertThat(pdf.length).isGreaterThan(500);
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        byte[] xlsx = exporter.exportXlsx(report, null);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetName(0)).isEqualTo("Resumen");
            assertThat(wb.getSheetName(1)).isEqualTo("Movimientos");
            assertThat(wb.getSheet("Movimientos").getRow(1).getCell(0).getStringCellValue()).startsWith("#BOL-");
        }
    }
}
