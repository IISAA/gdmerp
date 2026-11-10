package pe.edu.upeu.gdmerp.terceros.tercero.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.terceros.tercero.dto.TerceroRequest;
import pe.edu.upeu.gdmerp.terceros.tercero.dto.TerceroResponse;
import pe.edu.upeu.gdmerp.terceros.tercero.service.TerceroService;

import java.util.List;

@Tag(name = "CRM - Terceros Comerciales", description = "Gestión centralizada de Clientes y Proveedores con validación RUC/DNI")
@RestController("terceroController")
@RequestMapping("/api/v1/terceros")
@RequiredArgsConstructor
public class TerceroController {

    private final TerceroService terceroService;

    @Operation(summary = "Listar clientes y proveedores con filtro opcional por tipo (CLIENTE, PROVEEDOR, AMBOS, TODOS)")
    @GetMapping
    public ResponseEntity<List<TerceroResponse>> listar(@RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(terceroService.listar(tipo));
    }

    @Operation(summary = "Obtener un tercero por ID")
    @GetMapping("/{id}")
    public ResponseEntity<TerceroResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(terceroService.obtener(id));
    }

    @Operation(summary = "Registrar un nuevo tercero (Cliente o Proveedor)")
    @PostMapping
    public ResponseEntity<TerceroResponse> crear(@Valid @RequestBody TerceroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(terceroService.crear(request));
    }

    @Operation(summary = "Actualizar datos de un tercero")
    @PutMapping("/{id}")
    public ResponseEntity<TerceroResponse> actualizar(@PathVariable Long id, @Valid @RequestBody TerceroRequest request) {
        return ResponseEntity.ok(terceroService.actualizar(id, request));
    }

    @Operation(summary = "Dar de baja lógica a un tercero")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        terceroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
