package com.semillatecnologica.backend.modules.earnings.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EarningsReportResponse(
        EarningsPeriod period,
        int offset,
        String title,
        String rangeLabel,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        List<PeriodOption> selectorOptions,
        EarningsKpis kpis,
        List<ChartPoint> chartSeries,
        List<DistributionSlice> distribution,
        List<MovementRecord> movements
) {
    public record PeriodOption(
            int offset,
            String label
    ) {}

    public record EarningsKpis(
            BigDecimal totalEarnings,
            String earningsTrend,
            int ticketsSold,
            String ticketsTrend,
            BigDecimal averageTicket,
            String averageTrend,
            int transactionsCount,
            String rafflesCountLabel
    ) {}

    public record ChartPoint(
            String label,
            String subLabel,
            BigDecimal amount,
            int tickets
    ) {}

    public record DistributionSlice(
            String category,
            BigDecimal amount,
            int percentage,
            String color,
            String bgColor
    ) {}

    public record MovementRecord(
            String id,
            String folio,
            String raffleId,
            String concept,
            List<Integer> ticketNumbers,
            int tickets,
            String paymentMethod,
            String buyerName,
            String buyerEmail,
            LocalDateTime purchasedAt,
            String timestamp,
            BigDecimal amount
    ) {}
}
