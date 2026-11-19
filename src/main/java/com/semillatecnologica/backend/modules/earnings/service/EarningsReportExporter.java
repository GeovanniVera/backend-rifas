package com.semillatecnologica.backend.modules.earnings.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsPeriod;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsReportResponse;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsReportResponse.DistributionSlice;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsReportResponse.MovementRecord;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class EarningsReportExporter {

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter GENERATED_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color BRAND_DARK = new Color(11, 79, 50);     // #0B4F32
    private static final Color BRAND_LIGHT = new Color(236, 253, 245); // #ECFDF5
    private static final Color ROW_ALT = new Color(248, 250, 252);     // #F8FAFC
    private static final Color BORDER_GRAY = new Color(226, 232, 240); // #E2E8F0
    private static final Color TEXT_DARK = new Color(31, 41, 55);      // #1F2937
    private static final Color TEXT_MUTED = new Color(100, 116, 139);  // #64748B

    public byte[] exportPdf(EarningsReportResponse report, String paymentMethodFilter) {
        List<MovementRecord> movements = filterMovements(report.movements(), paymentMethodFilter);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 32, 32, 32, 32);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, new Color(209, 250, 229));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND_DARK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_MUTED);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, TEXT_DARK);
            Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
            Font tdFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_DARK);
            Font tdBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_DARK);

            // Encabezado
            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            header.setSpacingAfter(14f);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(BRAND_DARK);
            headerCell.setBorder(Rectangle.NO_BORDER);
            headerCell.setPadding(14f);

            Paragraph pTitle = new Paragraph("NUVORA — Reporte de Ganancias (" + periodNameEs(report.period()) + ")", titleFont);
            pTitle.setSpacingAfter(4f);
            headerCell.addElement(pTitle);

            String filterNote = hasFilter(paymentMethodFilter) ? "  |  Filtro: " + paymentMethodFilter : "";
            Paragraph pSub = new Paragraph(
                    report.title() + "  •  " + report.rangeLabel()
                            + "\nGenerado el " + LocalDateTime.now().format(GENERATED_FMT) + filterNote,
                    subTitleFont
            );
            headerCell.addElement(pSub);
            header.addCell(headerCell);
            document.add(header);

            // KPIs
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(14f);
            kpiTable.setWidths(new float[]{1.2f, 1f, 1f, 1f});

            addKpiCell(kpiTable, "GANANCIAS TOTALES", formatCurrency(report.kpis().totalEarnings()), labelFont, valueFont);
            addKpiCell(kpiTable, "BOLETOS VENDIDOS", String.valueOf(report.kpis().ticketsSold()), labelFont, valueFont);
            addKpiCell(kpiTable, "TICKET PROMEDIO", formatCurrency(report.kpis().averageTicket()), labelFont, valueFont);
            addKpiCell(kpiTable, "COMPRAS REALIZADAS", String.valueOf(report.kpis().transactionsCount()), labelFont, valueFont);
            document.add(kpiTable);

            // Distribución por método de pago
            Paragraph distHeading = new Paragraph("Distribución por Método de Pago", sectionFont);
            distHeading.setSpacingAfter(6f);
            document.add(distHeading);

            PdfPTable distTable = new PdfPTable(3);
            distTable.setWidthPercentage(100);
            distTable.setSpacingAfter(14f);
            distTable.setWidths(new float[]{2f, 1.5f, 1f});

            addTableHeader(distTable, new String[]{"Método de Pago", "Monto Recaudado", "Participación"}, thFont);
            for (int i = 0; i < report.distribution().size(); i++) {
                DistributionSlice slice = report.distribution().get(i);
                Color bg = (i % 2 == 1) ? ROW_ALT : Color.WHITE;
                addBodyCell(distTable, slice.category(), tdBoldFont, Element.ALIGN_LEFT, bg);
                addBodyCell(distTable, formatCurrency(slice.amount()), tdFont, Element.ALIGN_RIGHT, bg);
                addBodyCell(distTable, slice.percentage() + "%", tdFont, Element.ALIGN_RIGHT, bg);
            }
            document.add(distTable);

            // Tabla de Movimientos
            Paragraph movHeading = new Paragraph("Desglose de Movimientos (" + movements.size() + ")", sectionFont);
            movHeading.setSpacingAfter(6f);
            document.add(movHeading);

            PdfPTable movTable = new PdfPTable(7);
            movTable.setWidthPercentage(100);
            movTable.setWidths(new float[]{1.1f, 2.2f, 1.2f, 1.5f, 1.0f, 1.4f, 1.1f});

            addTableHeader(movTable, new String[]{"Folio", "Rifa", "Boletos", "Comprador", "Método", "Fecha", "Monto"}, thFont);

            BigDecimal filteredTotal = BigDecimal.ZERO;
            if (movements.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No hay movimientos registrados para este periodo.", tdFont));
                emptyCell.setColspan(7);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(12f);
                emptyCell.setBorderColor(BORDER_GRAY);
                movTable.addCell(emptyCell);
            } else {
                for (int i = 0; i < movements.size(); i++) {
                    MovementRecord m = movements.get(i);
                    filteredTotal = filteredTotal.add(m.amount());
                    Color bg = (i % 2 == 1) ? ROW_ALT : Color.WHITE;
                    String ticketsStr = m.ticketNumbers().stream()
                            .map(n -> "#" + n)
                            .collect(Collectors.joining(", "));

                    addBodyCell(movTable, m.folio(), tdBoldFont, Element.ALIGN_LEFT, bg);
                    addBodyCell(movTable, m.concept(), tdFont, Element.ALIGN_LEFT, bg);
                    addBodyCell(movTable, ticketsStr + " (" + m.tickets() + ")", tdFont, Element.ALIGN_LEFT, bg);
                    addBodyCell(movTable, m.buyerName(), tdFont, Element.ALIGN_LEFT, bg);
                    addBodyCell(movTable, m.paymentMethod(), tdFont, Element.ALIGN_CENTER, bg);
                    addBodyCell(movTable, m.timestamp(), tdFont, Element.ALIGN_LEFT, bg);
                    addBodyCell(movTable, formatCurrency(m.amount()), tdBoldFont, Element.ALIGN_RIGHT, bg);
                }

                // Fila total
                PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL DEL REPORTE", tdBoldFont));
                totalLabel.setColspan(6);
                totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalLabel.setBackgroundColor(BRAND_LIGHT);
                totalLabel.setBorderColor(BORDER_GRAY);
                totalLabel.setPadding(7f);
                movTable.addCell(totalLabel);

                addBodyCell(movTable, formatCurrency(filteredTotal), tdBoldFont, Element.ALIGN_RIGHT, BRAND_LIGHT);
            }

            document.add(movTable);
            document.close();
            return baos.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("No se pudo generar el reporte PDF: " + e.getMessage(), e);
        }
    }

    public byte[] exportXlsx(EarningsReportResponse report, String paymentMethodFilter) {
        List<MovementRecord> movements = filterMovements(report.movements(), paymentMethodFilter);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle labelStyle = createBoldStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook, false);
            CellStyle currencyBoldStyle = createCurrencyStyle(workbook, true);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);

            // Hoja 1: Resumen
            Sheet summary = workbook.createSheet("Resumen");
            int r = 0;

            org.apache.poi.ss.usermodel.Row titleRow = summary.createRow(r++);
            var titleCell = titleRow.createCell(0);
            titleCell.setCellValue("NUVORA — Reporte de Ganancias (" + periodNameEs(report.period()) + ")");
            titleCell.setCellStyle(titleStyle);

            r++;
            addMetaRow(summary, r++, "Periodo:", report.title(), labelStyle, textStyle);
            addMetaRow(summary, r++, "Rango de fechas:", report.rangeLabel(), labelStyle, textStyle);
            if (hasFilter(paymentMethodFilter)) {
                addMetaRow(summary, r++, "Filtro método:", paymentMethodFilter, labelStyle, textStyle);
            }
            addMetaRow(summary, r++, "Generado el:", LocalDateTime.now().format(GENERATED_FMT), labelStyle, textStyle);

            r++;
            var kpiHeader = summary.createRow(r++);
            createStyledCell(kpiHeader, 0, "Indicador", headerStyle);
            createStyledCell(kpiHeader, 1, "Valor", headerStyle);
            createStyledCell(kpiHeader, 2, "Detalle", headerStyle);

            addKpiXlsxRow(summary, r++, "Ganancias Totales", report.kpis().totalEarnings(), report.kpis().earningsTrend(), labelStyle, currencyBoldStyle, textStyle);
            addKpiXlsxIntRow(summary, r++, "Boletos Vendidos", report.kpis().ticketsSold(), report.kpis().ticketsTrend(), labelStyle, textStyle);
            addKpiXlsxRow(summary, r++, "Ticket Promedio", report.kpis().averageTicket(), report.kpis().averageTrend(), labelStyle, currencyStyle, textStyle);
            addKpiXlsxIntRow(summary, r++, "Compras Realizadas", report.kpis().transactionsCount(), report.kpis().rafflesCountLabel(), labelStyle, textStyle);

            r++;
            var distHeader = summary.createRow(r++);
            createStyledCell(distHeader, 0, "Método de Pago", headerStyle);
            createStyledCell(distHeader, 1, "Monto Recaudado", headerStyle);
            createStyledCell(distHeader, 2, "Porcentaje", headerStyle);

            for (DistributionSlice slice : report.distribution()) {
                var row = summary.createRow(r++);
                createStyledCell(row, 0, slice.category(), labelStyle);
                var amtCell = row.createCell(1);
                amtCell.setCellValue(slice.amount().doubleValue());
                amtCell.setCellStyle(currencyStyle);
                createStyledCell(row, 2, slice.percentage() + "%", textStyle);
            }

            for (int c = 0; c < 3; c++) {
                summary.autoSizeColumn(c);
                summary.setColumnWidth(c, Math.max(summary.getColumnWidth(c) + 800, 5000));
            }

            // Hoja 2: Movimientos
            Sheet movSheet = workbook.createSheet("Movimientos");
            String[] headers = {"Folio", "Rifa", "Boletos", "Cantidad", "Comprador", "Correo", "Método de Pago", "Fecha y Hora", "Monto (MXN)"};
            var mh = movSheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                createStyledCell(mh, c, headers[c], headerStyle);
            }

            int mr = 1;
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (MovementRecord m : movements) {
                totalAmount = totalAmount.add(m.amount());
                var row = movSheet.createRow(mr++);
                String ticketsStr = m.ticketNumbers().stream()
                        .map(n -> "#" + n)
                        .collect(Collectors.joining(", "));

                createStyledCell(row, 0, m.folio(), labelStyle);
                createStyledCell(row, 1, m.concept(), textStyle);
                createStyledCell(row, 2, ticketsStr, textStyle);
                var qtyCell = row.createCell(3);
                qtyCell.setCellValue(m.tickets());
                qtyCell.setCellStyle(textStyle);
                createStyledCell(row, 4, m.buyerName(), textStyle);
                createStyledCell(row, 5, m.buyerEmail(), textStyle);
                createStyledCell(row, 6, m.paymentMethod(), textStyle);
                createStyledCell(row, 7, m.timestamp(), textStyle);

                var amtCell = row.createCell(8);
                amtCell.setCellValue(m.amount().doubleValue());
                amtCell.setCellStyle(currencyStyle);
            }

            var totalRow = movSheet.createRow(mr);
            createStyledCell(totalRow, 7, "TOTAL", totalLabelStyle);
            var totalCell = totalRow.createCell(8);
            totalCell.setCellValue(totalAmount.doubleValue());
            totalCell.setCellStyle(currencyBoldStyle);

            for (int c = 0; c < headers.length; c++) {
                movSheet.autoSizeColumn(c);
                movSheet.setColumnWidth(c, Math.max(movSheet.getColumnWidth(c) + 600, 3800));
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte Excel: " + e.getMessage(), e);
        }
    }

    private List<MovementRecord> filterMovements(List<MovementRecord> movements, String paymentMethodFilter) {
        if (!hasFilter(paymentMethodFilter)) return movements;
        String target = paymentMethodFilter.trim();
        return movements.stream()
                .filter(m -> m.paymentMethod().equalsIgnoreCase(target))
                .toList();
    }

    private boolean hasFilter(String filter) {
        return filter != null && !filter.isBlank() && !"TODOS".equalsIgnoreCase(filter.trim());
    }

    private String periodNameEs(EarningsPeriod period) {
        return switch (period) {
            case DAY -> "DÍA";
            case WEEK -> "SEMANA";
            case FORTNIGHT -> "QUINCENA";
        };
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(ES_MX);
        fmt.setMaximumFractionDigits(0);
        return fmt.format(amount != null ? amount : BigDecimal.ZERO);
    }

    private void addKpiCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ROW_ALT);
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(9f);

        Paragraph pLabel = new Paragraph(label, labelFont);
        pLabel.setSpacingAfter(4f);
        Paragraph pValue = new Paragraph(value, valueFont);

        cell.addElement(pLabel);
        cell.addElement(pValue);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String[] cols, Font font) {
        for (String col : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(col, font));
            cell.setBackgroundColor(BRAND_DARK);
            cell.setBorderColor(BRAND_DARK);
            cell.setPadding(6f);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private void addBodyCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addMetaRow(Sheet sheet, int rowIdx, String label, String value, CellStyle labelStyle, CellStyle valueStyle) {
        var row = sheet.createRow(rowIdx);
        createStyledCell(row, 0, label, labelStyle);
        createStyledCell(row, 1, value, valueStyle);
    }

    private void addKpiXlsxRow(Sheet sheet, int rowIdx, String label, BigDecimal amount, String detail,
                               CellStyle labelStyle, CellStyle currencyStyle, CellStyle textStyle) {
        var row = sheet.createRow(rowIdx);
        createStyledCell(row, 0, label, labelStyle);
        var c = row.createCell(1);
        c.setCellValue(amount != null ? amount.doubleValue() : 0.0);
        c.setCellStyle(currencyStyle);
        createStyledCell(row, 2, detail, textStyle);
    }

    private void addKpiXlsxIntRow(Sheet sheet, int rowIdx, String label, int val, String detail,
                                  CellStyle labelStyle, CellStyle textStyle) {
        var row = sheet.createRow(rowIdx);
        createStyledCell(row, 0, label, labelStyle);
        var c = row.createCell(1);
        c.setCellValue(val);
        c.setCellStyle(labelStyle);
        createStyledCell(row, 2, detail, textStyle);
    }

    private void createStyledCell(org.apache.poi.ss.usermodel.Row row, int col, String value, CellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle createTextStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb, boolean bold) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        if (bold) {
            XSSFFont font = (XSSFFont) wb.createFont();
            font.setBold(true);
            style.setFont(font);
        }
        return style;
    }

    private CellStyle createTotalLabelStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
