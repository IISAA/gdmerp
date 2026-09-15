package pe.edu.upeu.gdmerp.inventario.producto.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockProductoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoReporteService;
import java.util.List;

@Tag(name = "Reportes de Productos")
@RestController
@RequestMapping("/api/v1/productos/reportes")
@RequiredArgsConstructor
public class ProductoReporteController {

    private final ProductoReporteService reporteService;

    @Operation(summary = "Reporte de stock consolidado por producto")
    @GetMapping("/stock")
    public ResponseEntity<List<ReporteStockProductoDTO>> stockConsolidado() {
        return ResponseEntity.ok(reporteService.obtenerReporteStock());
    }

    @Operation(summary = "Reporte de stock crítico (bajo stock mínimo)")
    @GetMapping("/stock-critico")
    public ResponseEntity<List<ReporteStockCriticoDTO>> stockCritico() {
        return ResponseEntity.ok(reporteService.obtenerReporteStockCritico());
    }
}