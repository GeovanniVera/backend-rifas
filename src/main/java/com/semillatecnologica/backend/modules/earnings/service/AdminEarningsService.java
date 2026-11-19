package com.semillatecnologica.backend.modules.earnings.service;

import com.semillatecnologica.backend.modules.auth.model.User;
import com.semillatecnologica.backend.modules.auth.repository.UserRepository;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsPeriod;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsReportResponse;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsReportResponse.*;
import com.semillatecnologica.backend.modules.raffles.model.Raffle;
import com.semillatecnologica.backend.modules.raffles.model.RaffleTicket;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleRepository;
import com.semillatecnologica.backend.modules.raffles.repository.RaffleTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminEarningsService {

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd MMM", ES_MX);
    private static final DateTimeFormatter FULL_DATE_FMT = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM yyyy", ES_MX);
    private static final DateTimeFormatter MONTH_YEAR_FMT = DateTimeFormatter.ofPattern("MMM yyyy", ES_MX);
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", ES_MX);

    private final RaffleTicketRepository ticketRepository;
    private final RaffleRepository raffleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public EarningsReportResponse getEarnings(EarningsPeriod period, int rawOffset) {
        int offset = Math.max(0, Math.min(rawOffset, 24));
        LocalDate today = LocalDate.now();

        PeriodRange currentRange = resolveRange(period, today, offset);
        PeriodRange previousRange = resolveRange(period, today, offset + 1);

        List<RaffleTicket> currentTickets = ticketRepository
                .findByStatusAndPurchasedAtBetweenOrderByPurchasedAtDesc(
                        RaffleTicket.Status.SOLD, currentRange.start(), currentRange.end());

        List<RaffleTicket> previousTickets = ticketRepository
                .findByStatusAndPurchasedAtBetweenOrderByPurchasedAtDesc(
                        RaffleTicket.Status.SOLD, previousRange.start(), previousRange.end());

        Set<String> raffleIds = new HashSet<>();
        Set<String> userIds = new HashSet<>();
        for (RaffleTicket t : currentTickets) {
            raffleIds.add(t.getRaffleId());
            if (t.getUserId() != null) userIds.add(t.getUserId());
        }
        for (RaffleTicket t : previousTickets) {
            raffleIds.add(t.getRaffleId());
        }

        Map<String, Raffle> rafflesMap = raffleRepository.findAllById(raffleIds).stream()
                .collect(Collectors.toMap(Raffle::getId, Function.identity()));

        Map<String, User> usersMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        BigDecimal totalEarnings = sumTicketsAmount(currentTickets, rafflesMap);
        BigDecimal previousEarnings = sumTicketsAmount(previousTickets, rafflesMap);

        List<MovementRecord> movements = buildMovements(currentTickets, rafflesMap, usersMap);
        int ticketsSold = currentTickets.size();
        int transactionsCount = movements.size();

        BigDecimal averageTicket = transactionsCount > 0
                ? totalEarnings.divide(BigDecimal.valueOf(transactionsCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long distinctRaffles = currentTickets.stream()
                .map(RaffleTicket::getRaffleId)
                .distinct()
                .count();

        String earningsTrend = formatTrend(totalEarnings, previousEarnings, period);
        String ticketsTrend = ticketsSold == 1 ? "1 boleto vendido" : ticketsSold + " boletos vendidos";
        String averageTrend = transactionsCount == 1 ? "1 compra" : transactionsCount + " compras";
        String rafflesLabel = distinctRaffles == 1 ? "1 rifa con ventas" : distinctRaffles + " rifas con ventas";

        EarningsKpis kpis = new EarningsKpis(
                totalEarnings,
                earningsTrend,
                ticketsSold,
                ticketsTrend,
                averageTicket,
                averageTrend,
                transactionsCount,
                rafflesLabel
        );

        List<ChartPoint> chartSeries = buildChartSeries(period, currentRange, currentTickets, rafflesMap);
        List<DistributionSlice> distribution = buildDistribution(currentTickets, rafflesMap, totalEarnings);
        List<PeriodOption> selectorOptions = buildSelectorOptions(period, today);

        return new EarningsReportResponse(
                period,
                offset,
                currentRange.title(),
                currentRange.rangeLabel(),
                currentRange.start(),
                currentRange.end(),
                selectorOptions,
                kpis,
                chartSeries,
                distribution,
                movements
        );
    }

    private BigDecimal ticketPriceOf(RaffleTicket ticket, Map<String, Raffle> rafflesMap) {
        Raffle raffle = rafflesMap.get(ticket.getRaffleId());
        return raffle != null && raffle.getTicketPrice() != null ? raffle.getTicketPrice() : BigDecimal.ZERO;
    }

    private BigDecimal sumTicketsAmount(List<RaffleTicket> tickets, Map<String, Raffle> rafflesMap) {
        BigDecimal total = BigDecimal.ZERO;
        for (RaffleTicket t : tickets) {
            total = total.add(ticketPriceOf(t, rafflesMap));
        }
        return total;
    }

    private List<MovementRecord> buildMovements(List<RaffleTicket> tickets,
                                                Map<String, Raffle> rafflesMap,
                                                Map<String, User> usersMap) {
        // Preservar orden descendente por purchasedAt
        Map<String, List<RaffleTicket>> grouped = new LinkedHashMap<>();
        for (RaffleTicket t : tickets) {
            String key = (t.getPaymentIntentId() != null && !t.getPaymentIntentId().isBlank())
                    ? t.getPaymentIntentId() + "::" + t.getRaffleId()
                    : t.getId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        List<MovementRecord> result = new ArrayList<>();
        for (Map.Entry<String, List<RaffleTicket>> entry : grouped.entrySet()) {
            List<RaffleTicket> group = entry.getValue();
            RaffleTicket sample = group.get(0);
            Raffle raffle = rafflesMap.get(sample.getRaffleId());
            User buyer = sample.getUserId() != null ? usersMap.get(sample.getUserId()) : null;

            List<Integer> ticketNumbers = group.stream()
                    .map(RaffleTicket::getTicketNumber)
                    .sorted()
                    .toList();

            BigDecimal amount = sumTicketsAmount(group, rafflesMap);
            String paymentMethod = normalizePaymentMethod(sample.getPaymentMethod());
            LocalDateTime purchasedAt = sample.getPurchasedAt();
            String timestamp = purchasedAt != null ? purchasedAt.format(TIMESTAMP_FMT) : "—";

            String rawRef = sample.getPaymentIntentId() != null ? sample.getPaymentIntentId() : sample.getId();
            String shortRef = rawRef.length() > 6
                    ? rawRef.substring(rawRef.length() - 6).toUpperCase()
                    : rawRef.toUpperCase();
            String folio = "#BOL-" + shortRef;

            result.add(new MovementRecord(
                    entry.getKey(),
                    folio,
                    sample.getRaffleId(),
                    raffle != null ? raffle.getName() : "Rifa",
                    ticketNumbers,
                    group.size(),
                    paymentMethod,
                    buyer != null ? buyer.getName() : "Cliente",
                    buyer != null ? buyer.getEmail() : "—",
                    purchasedAt,
                    timestamp,
                    amount
            ));
        }

        return result;
    }

    private List<ChartPoint> buildChartSeries(EarningsPeriod period,
                                              PeriodRange range,
                                              List<RaffleTicket> tickets,
                                              Map<String, Raffle> rafflesMap) {
        return switch (period) {
            case DAY -> buildDaySeries(tickets, rafflesMap);
            case WEEK -> buildWeekSeries(range.startDate(), tickets, rafflesMap);
            case FORTNIGHT -> buildFortnightSeries(range.startDate(), range.endDate(), tickets, rafflesMap);
        };
    }

    private List<ChartPoint> buildDaySeries(List<RaffleTicket> tickets, Map<String, Raffle> rafflesMap) {
        // 8 franjas de 3 horas cubriendo las 24 horas del día
        List<ChartPoint> points = new ArrayList<>();
        for (int block = 0; block < 8; block++) {
            int startHour = block * 3;
            int endHour = startHour + 3;
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (RaffleTicket t : tickets) {
                if (t.getPurchasedAt() != null) {
                    int h = t.getPurchasedAt().getHour();
                    if (h >= startHour && h < endHour) {
                        sum = sum.add(ticketPriceOf(t, rafflesMap));
                        count++;
                    }
                }
            }
            String label = String.format("%02d:00", startHour);
            String subLabel = String.format("%02d:00 - %02d:59 hrs", startHour, endHour - 1);
            points.add(new ChartPoint(label, subLabel, sum, count));
        }
        return points;
    }

    private List<ChartPoint> buildWeekSeries(LocalDate weekStart,
                                             List<RaffleTicket> tickets,
                                             Map<String, Raffle> rafflesMap) {
        String[] dayNames = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        DateTimeFormatter fullDayFmt = DateTimeFormatter.ofPattern("EEEE dd MMM", ES_MX);
        List<ChartPoint> points = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (RaffleTicket t : tickets) {
                if (t.getPurchasedAt() != null && t.getPurchasedAt().toLocalDate().equals(day)) {
                    sum = sum.add(ticketPriceOf(t, rafflesMap));
                    count++;
                }
            }
            String sub = capitalize(day.format(fullDayFmt));
            points.add(new ChartPoint(dayNames[i], sub, sum, count));
        }
        return points;
    }

    private List<ChartPoint> buildFortnightSeries(LocalDate startDate,
                                                  LocalDate endDate,
                                                  List<RaffleTicket> tickets,
                                                  Map<String, Raffle> rafflesMap) {
        List<ChartPoint> points = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            LocalDate day = cursor;
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (RaffleTicket t : tickets) {
                if (t.getPurchasedAt() != null && t.getPurchasedAt().toLocalDate().equals(day)) {
                    sum = sum.add(ticketPriceOf(t, rafflesMap));
                    count++;
                }
            }
            String label = "D" + day.getDayOfMonth();
            String sub = day.format(SHORT_DATE_FMT);
            points.add(new ChartPoint(label, sub, sum, count));
            cursor = cursor.plusDays(1);
        }
        return points;
    }

    private List<DistributionSlice> buildDistribution(List<RaffleTicket> tickets,
                                                      Map<String, Raffle> rafflesMap,
                                                      BigDecimal totalEarnings) {
        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        byMethod.put("Tarjeta", BigDecimal.ZERO);
        byMethod.put("OXXO", BigDecimal.ZERO);
        byMethod.put("Transferencia", BigDecimal.ZERO);

        for (RaffleTicket t : tickets) {
            String method = normalizePaymentMethod(t.getPaymentMethod());
            BigDecimal price = ticketPriceOf(t, rafflesMap);
            byMethod.merge(method, price, BigDecimal::add);
        }

        // Eliminar categorías extra en 0 si no hubo ventas de ese método, pero mantener Tarjeta y OXXO
        Map<String, String> colors = Map.of(
                "Tarjeta", "#10B981",
                "OXXO", "#F59E0B",
                "Transferencia", "#6366F1"
        );
        Map<String, String> bgColors = Map.of(
                "Tarjeta", "#ECFDF5",
                "OXXO", "#FFFBEB",
                "Transferencia", "#EEF2FF"
        );

        List<DistributionSlice> slices = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : byMethod.entrySet()) {
            BigDecimal amount = entry.getValue();
            int pct = totalEarnings.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(BigDecimal.valueOf(100))
                            .divide(totalEarnings, 0, RoundingMode.HALF_UP)
                            .intValue()
                    : 0;

            slices.add(new DistributionSlice(
                    entry.getKey(),
                    amount,
                    pct,
                    colors.getOrDefault(entry.getKey(), "#6366F1"),
                    bgColors.getOrDefault(entry.getKey(), "#EEF2FF")
            ));
        }

        return slices;
    }

    private List<PeriodOption> buildSelectorOptions(EarningsPeriod period, LocalDate today) {
        List<PeriodOption> options = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            PeriodRange range = resolveRange(period, today, i);
            options.add(new PeriodOption(i, range.optionLabel()));
        }
        return options;
    }

    private PeriodRange resolveRange(EarningsPeriod period, LocalDate today, int offset) {
        return switch (period) {
            case DAY -> {
                LocalDate d = today.minusDays(offset);
                String prefix = offset == 0 ? "Hoy" : offset == 1 ? "Ayer" : capitalize(d.format(DateTimeFormatter.ofPattern("EEEE", ES_MX)));
                String optionLabel = prefix + " · " + d.format(SHORT_DATE_FMT);
                yield new PeriodRange(
                        d,
                        d,
                        "Corte Diario por Franja Horaria",
                        capitalize(d.format(FULL_DATE_FMT)),
                        optionLabel
                );
            }
            case WEEK -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset);
                LocalDate sunday = monday.plusDays(6);
                String prefix = offset == 0 ? "Semana Actual" : offset == 1 ? "Semana Pasada" : "Hace " + offset + " semanas";
                String span = monday.format(SHORT_DATE_FMT) + " - " + sunday.format(SHORT_DATE_FMT);
                yield new PeriodRange(
                        monday,
                        sunday,
                        "Acumulado Semanal (Lun a Dom)",
                        span + " " + sunday.getYear(),
                        prefix + " (" + span + ")"
                );
            }
            case FORTNIGHT -> {
                LocalDate start = today.getDayOfMonth() <= 15
                        ? today.withDayOfMonth(1)
                        : today.withDayOfMonth(16);

                for (int step = 0; step < offset; step++) {
                    if (start.getDayOfMonth() == 16) {
                        start = start.withDayOfMonth(1);
                    } else {
                        LocalDate prevMonth = start.minusMonths(1);
                        start = prevMonth.withDayOfMonth(16);
                    }
                }

                LocalDate end = start.getDayOfMonth() == 1
                        ? start.withDayOfMonth(15)
                        : start.withDayOfMonth(start.lengthOfMonth());

                String halfLabel = start.getDayOfMonth() == 1 ? "1ª Quincena" : "2ª Quincena";
                String monthStr = capitalize(start.format(MONTH_YEAR_FMT));
                String span = start.format(SHORT_DATE_FMT) + " - " + end.format(SHORT_DATE_FMT);
                String optionLabel = (offset == 0 ? "Quincena Actual" : halfLabel + " " + monthStr) + " (" + span + ")";

                yield new PeriodRange(
                        start,
                        end,
                        "Corte Quincenal (" + halfLabel + ")",
                        halfLabel + " · " + span + " " + end.getYear(),
                        optionLabel
                );
            }
        };
    }

    private String formatTrend(BigDecimal current, BigDecimal previous, EarningsPeriod period) {
        String comp = switch (period) {
            case DAY -> "vs ayer";
            case WEEK -> "vs sem. ant.";
            case FORTNIGHT -> "vs quinc. ant.";
        };

        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0
                    ? "Nuevo ingreso " + comp
                    : "Sin movimientos " + comp;
        }

        BigDecimal diff = current.subtract(previous);
        BigDecimal pct = diff.multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
        String sign = pct.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return sign + pct + "% " + comp;
    }

    private String normalizePaymentMethod(String method) {
        if (method == null || method.isBlank()) return "Tarjeta";
        return switch (method.trim().toUpperCase()) {
            case "CARD" -> "Tarjeta";
            case "OXXO" -> "OXXO";
            case "TRANSFER" -> "Transferencia";
            default -> method;
        };
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private record PeriodRange(
            LocalDate startDate,
            LocalDate endDate,
            String title,
            String rangeLabel,
            String optionLabel
    ) {
        LocalDateTime start() {
            return startDate.atStartOfDay();
        }

        LocalDateTime end() {
            return endDate.plusDays(1).atStartOfDay().minusNanos(1);
        }
    }
}
