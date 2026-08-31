package pe.edu.upeu.gdmerp.produccion.ordenproduccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.service.OrdenProduccionService;
import java.util.List;

@Tag(name = "Órdenes de Producción")
@RestController
@RequestMapping("/api/v1/ordenes-produccion")
@RequiredArgsConstructor
public class OrdenProduccionController {
    private final OrdenProduccionService ordenProduccionService;

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
    public ResponseEntity<OrdenProduccionResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody OrdenProduccionRequest request) {
        return ResponseEntity.ok(ordenProduccionService.actualizar(id, request));
    }

    @Operation(summary = "Elimina una orden de producción")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ordenProduccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}