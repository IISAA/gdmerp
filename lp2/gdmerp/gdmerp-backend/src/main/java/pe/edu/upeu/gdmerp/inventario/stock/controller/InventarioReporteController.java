package pe.edu.upeu.gdmerp.inventario.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteStockProductoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.service.InventarioService;

import java.util.List;

@Tag(name = "Inventario - Reportes")
@RestController
@RequestMapping({"/api/v1/inventario/reportes", "/api/v1/productos/reportes"})
@RequiredArgsConstructor
public class InventarioReporteController {

    private final InventarioService inventarioService;

    @Operation(summary = "Reporte de stock consolidado por producto")
    @GetMapping("/stock")
    public ResponseEntity<List<ReporteStockProductoDTO>> stockConsolidado() {
        return ResponseEntity.ok(inventarioService.obtenerReporteStock());
    }

    @Operation(summary = "Reporte de stock crítico (bajo stock mínimo)")
    @GetMapping("/stock-critico")
    public ResponseEntity<List<ReporteStockCriticoDTO>> stockCritico() {
        return ResponseEntity.ok(inventarioService.obtenerReporteStockCritico());
    }

    @Operation(summary = "Reporte semáforo de inventario y vencimiento de lotes")
    @GetMapping({"/reporte-semaforo", "/semaforo"})
    public ResponseEntity<pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteSemaforoDTO> reporteSemaforo() {
        return ResponseEntity.ok(inventarioService.obtenerReporteSemaforo());
    }
}
