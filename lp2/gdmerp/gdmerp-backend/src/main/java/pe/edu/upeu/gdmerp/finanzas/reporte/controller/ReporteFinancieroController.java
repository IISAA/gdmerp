package pe.edu.upeu.gdmerp.finanzas.reporte.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.gdmerp.finanzas.exporter.CierreCajaExportService;
import pe.edu.upeu.gdmerp.finanzas.exporter.CierreCajaExporter;
import pe.edu.upeu.gdmerp.finanzas.exporter.FormatoReporte;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.DashboardFinancieroResponse;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.FlujoCajaResumenResponse;
import pe.edu.upeu.gdmerp.finanzas.reporte.service.ReporteFinancieroService;

@RestController
@RequestMapping("/api/v1/finanzas/reportes")
public class ReporteFinancieroController {

    private final ReporteFinancieroService reporteService;
    private final CierreCajaExportService exportService;

    public ReporteFinancieroController(
            ReporteFinancieroService reporteService,
            CierreCajaExportService exportService) {
        this.reporteService = reporteService;
        this.exportService = exportService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardFinancieroResponse> obtenerDashboard() {
        return ResponseEntity.ok(reporteService.obtenerDashboard());
    }

    @GetMapping("/flujo-caja")
    public ResponseEntity<FlujoCajaResumenResponse> obtenerFlujoCaja() {
        return ResponseEntity.ok(reporteService.obtenerFlujoCaja());
    }

    @GetMapping("/rendiciones-agregado")
    public ResponseEntity<ReporteRendicionesAgregadoDto> obtenerRendicionesAgregado() {
        return ResponseEntity.ok(reporteService.obtenerReporteRendiciones());
    }

    @GetMapping("/cierre-caja/{sesionId}/exportar")
    public ResponseEntity<byte[]> exportarCierreCaja(
            @PathVariable Long sesionId,
            @RequestParam(defaultValue = "PDF") FormatoReporte formato) {

        byte[] contenido = reporteService.exportarCierreCaja(sesionId, formato);
        CierreCajaExporter exporter = exportService.resolverExportador(formato);

        String filename = "cierre-caja-sesion-" + sesionId + "." + exporter.getFileExtension();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, exporter.getContentType())
                .body(contenido);
    }
}
