package pe.edu.upeu.gdmerp.produccion.ordenproduccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ReporteConsumoOrdenDTO;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ResumenEstadoOrdenesDTO;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.service.OrdenProduccionReporteService;
import java.util.List;

@Tag(name = "Reportes de Órdenes de Producción")
@RestController
@RequestMapping("/api/v1/produccion/ordenes/reportes")
@RequiredArgsConstructor
public class OrdenProduccionReporteController {

    private final OrdenProduccionReporteService reporteService;

    @Operation(summary = "Resumen de órdenes de producción por estado")
    @GetMapping("/estados")
    public ResponseEntity<List<ResumenEstadoOrdenesDTO>> resumenEstados() {
        return ResponseEntity.ok(reporteService.obtenerResumenEstados());
    }

    @Operation(summary = "Costo/consumo de materiales de una orden de producción")
    @GetMapping("/costo-orden/{ordenId}")
    public ResponseEntity<ReporteConsumoOrdenDTO> costoConsumo(@PathVariable Long ordenId) {
        return ResponseEntity.ok(reporteService.calcularConsumo(ordenId));
    }
}