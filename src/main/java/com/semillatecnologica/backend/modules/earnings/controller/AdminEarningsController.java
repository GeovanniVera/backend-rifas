package com.semillatecnologica.backend.modules.earnings.controller;

import com.semillatecnologica.backend.modules.earnings.dto.EarningsPeriod;
import com.semillatecnologica.backend.modules.earnings.dto.EarningsReportResponse;
import com.semillatecnologica.backend.modules.earnings.service.AdminEarningsService;
import com.semillatecnologica.backend.modules.earnings.service.EarningsReportExporter;
import com.semillatecnologica.backend.shared.exception.ValidationException;
import com.semillatecnologica.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/earnings")
@RequiredArgsConstructor
@Tag(name = "Ganancias Admin", description = "Revisión de ganancias por día, semana y quincena y exportación PDF/XLSX")
public class AdminEarningsController {

    private final AdminEarningsService earningsService;
    private final EarningsReportExporter reportExporter;

    @GetMapping
    @PreAuthorize("hasAuthority('raffles.read')")
    @Operation(summary = "Consultar ganancias por periodo",
               description = "Requiere permiso `raffles.read`. Devuelve KPIs, serie gráfica, distribución y movimientos.")
    public ResponseEntity<ApiResponse<EarningsReportResponse>> getEarnings(
            @RequestParam(defaultValue = "DAY") EarningsPeriod period,
            @RequestParam(defaultValue = "0") int offset) {
        EarningsReportResponse report = earningsService.getEarnings(period, offset);
        return ResponseEntity.ok(ApiResponse.ok("Ganancias del periodo", report));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('raffles.read')")
    @Operation(summary = "Exportar reporte de ganancias en PDF o XLSX",
               description = "Requiere permiso `raffles.read`. Genera un archivo descargable en formato pdf o xlsx.")
    public ResponseEntity<byte[]> exportEarnings(
            @RequestParam(defaultValue = "DAY") EarningsPeriod period,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String paymentMethod) {

        EarningsReportResponse report = earningsService.getEarnings(period, offset);
        String normalizedFormat = format.trim().toLowerCase();
        String baseFilename = "ganancias_" + period.name().toLowerCase() + "_" + report.rangeStart().toLocalDate();

        if ("pdf".equals(normalizedFormat)) {
            byte[] pdfBytes = reportExporter.exportPdf(report, paymentMethod);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseFilename + ".pdf\"")
                    .body(pdfBytes);
        }

        if ("xlsx".equals(normalizedFormat)) {
            byte[] xlsxBytes = reportExporter.exportXlsx(report, paymentMethod);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseFilename + ".xlsx\"")
                    .body(xlsxBytes);
        }

        throw new ValidationException("Formato no soportado. Usa 'pdf' o 'xlsx'.");
    }
}
