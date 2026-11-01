package pe.edu.upeu.gdmerp.produccion.ordenproduccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionUpdateRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.service.OrdenProduccionService;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.service.ReportePdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;

@Tag(name = "Órdenes de Producción")
@RestController
@RequestMapping("/api/v1/ordenes-produccion")
@RequiredArgsConstructor
public class OrdenProduccionController {
    private final OrdenProduccionService ordenProduccionService;
    private final OrdenProduccionRepository ordenRepository;
    private final ReportePdfService reportePdfService;

    @Operation(summary = "Lista todas las órdenes de producción")
    @GetMapping
    public ResponseEntity<List<OrdenProduccionResponse>> listar() {
        return ResponseEntity.ok(ordenProduccionService.listar());
    }

    @Operation(summary = "Navegación controlada: lista las órdenes de un centro de trabajo")
    @GetMapping("/por-centro-trabajo/{centroTrabajoId}")
    public ResponseEntity<List<OrdenProduccionResponse>> listarPorCentroTrabajo(@PathVariable Long centroTrabajoId) {
        return ResponseEntity.ok(ordenProduccionService.listarPorCentroTrabajo(centroTrabajoId));
    }

    @Operation(summary = "Busca orden de producción por ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrdenProduccionResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordenProduccionService.buscarPorId(id));
    }

    @Operation(summary = "Crea una orden de producción")
    @PostMapping
    public ResponseEntity<OrdenProduccionResponse> crear(@Valid @RequestBody OrdenProduccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenProduccionService.crear(request));
    }

    @Operation(summary = "Actualiza una orden de producción")
    @PutMapping("/{id}")
    public ResponseEntity<OrdenProduccionResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody OrdenProduccionUpdateRequest request) {
        return ResponseEntity.ok(ordenProduccionService.actualizar(id, request));
    }

    @Operation(summary = "Inicia la producción cambiando el estado a EN_PROCESO y descontando insumos según receta (BOM)")
    @PutMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ResponseEntity<OrdenProduccionResponse> iniciarProduccion(@PathVariable Long id) {
        return ResponseEntity.ok(ordenProduccionService.iniciarProduccion(id));
    }

    @Operation(summary = "Procesa la transición de estado de una orden de producción vinculada a Kardex")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO')")
    public ResponseEntity<OrdenProduccionResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoOrdenProduccion nuevoEstado) {
        return ResponseEntity.ok(ordenProduccionService.procesarTransicionEstado(id, nuevoEstado));
    }

    @Operation(summary = "Elimina una orden de producción")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ordenProduccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Genera el reporte oficial en PDF de la orden de producción")
    @GetMapping(value = "/{id}/reporte-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> obtenerReportePdf(@PathVariable Long id) {
        OrdenProduccion orden = ordenRepository.findByIdWithRelaciones(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de producción no encontrada con ID: " + id));

        byte[] pdfBytes = reportePdfService.generarReporteOrdenProduccion(orden);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Orden_" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
