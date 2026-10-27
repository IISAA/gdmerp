package pe.edu.upeu.gdmerp.produccion.centrotrabajo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoRequest;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.service.CentroTrabajoService;
import java.util.List;

@Tag(name = "Centros de Trabajo")
@RestController
@RequestMapping("/api/v1/centros-trabajo")
@RequiredArgsConstructor
public class CentroTrabajoController {
    private final CentroTrabajoService centroTrabajoService;

    @Operation(summary = "Lista todos los centros de trabajo")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public ResponseEntity<List<CentroTrabajoResponse>> listar() {
        return ResponseEntity.ok(centroTrabajoService.listar());
    }

    @Operation(summary = "Busca centro de trabajo por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public ResponseEntity<CentroTrabajoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(centroTrabajoService.buscarPorId(id));
    }

    @Operation(summary = "Crea un centro de trabajo")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ResponseEntity<CentroTrabajoResponse> crear(@Valid @RequestBody CentroTrabajoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(centroTrabajoService.crear(request));
    }

    @Operation(summary = "Actualiza un centro de trabajo")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ResponseEntity<CentroTrabajoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody CentroTrabajoRequest request) {
        return ResponseEntity.ok(centroTrabajoService.actualizar(id, request));
    }

    @Operation(summary = "Elimina un centro de trabajo")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        centroTrabajoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
