package pe.edu.upeu.gdmerp.produccion.listamateriales.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesRequest;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.service.ListaMaterialesService;

import java.util.List;

@Tag(name = "Lista de Materiales (BOM / Recetas)")
@RestController
@RequestMapping({"/api/v1/listas-materiales", "/api/v1/produccion/listas-materiales"})
@RequiredArgsConstructor
public class ListaMaterialesController {

    private final ListaMaterialesService listaMaterialesService;

    @Operation(summary = "Lista todas las recetas de materiales")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public ResponseEntity<List<ListaMaterialesResponse>> listar() {
        return ResponseEntity.ok(listaMaterialesService.listar());
    }

    @Operation(summary = "Obtiene una receta por su ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public ResponseEntity<ListaMaterialesResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(listaMaterialesService.buscarPorId(id));
    }

    @Operation(summary = "Obtiene las recetas asociadas a un producto terminado")
    @GetMapping("/por-producto/{productoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'ALMACENERO')")
    public ResponseEntity<List<ListaMaterialesResponse>> buscarPorProductoTerminado(@PathVariable Long productoId) {
        return ResponseEntity.ok(listaMaterialesService.listarPorProductoTerminadoId(productoId));
    }

    @Operation(summary = "Crea una nueva lista de materiales")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ResponseEntity<ListaMaterialesResponse> crear(@Valid @RequestBody ListaMaterialesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listaMaterialesService.crear(request));
    }

    @Operation(summary = "Actualiza una lista de materiales existente")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ResponseEntity<ListaMaterialesResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ListaMaterialesRequest request) {
        return ResponseEntity.ok(listaMaterialesService.actualizar(id, request));
    }

    @Operation(summary = "Elimina una lista de materiales")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        listaMaterialesService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
