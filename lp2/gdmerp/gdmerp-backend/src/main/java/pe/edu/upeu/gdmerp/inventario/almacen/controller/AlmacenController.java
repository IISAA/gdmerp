package pe.edu.upeu.gdmerp.inventario.almacen.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenRequest;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.almacen.service.AlmacenService;
import java.util.List;

@Tag(name = "Almacenes")
@RestController
@RequestMapping("/api/v1/almacenes")
@RequiredArgsConstructor
public class AlmacenController {
    private final AlmacenService almacenService;

    @Operation(summary = "Lista todos los almacenes")
    @GetMapping
    public ResponseEntity<List<AlmacenResponse>> listar() {
        return ResponseEntity.ok(almacenService.listar());
    }

    @Operation(summary = "Busca un almacén por ID")
    @GetMapping("/{id}")
    public ResponseEntity<AlmacenResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(almacenService.buscarPorId(id));
    }

    @Operation(summary = "Crea un nuevo almacén")
    @PostMapping
    public ResponseEntity<AlmacenResponse> crear(@Valid @RequestBody AlmacenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(almacenService.crear(request));
    }

    @Operation(summary = "Actualiza un almacén existente")
    @PutMapping("/{id}")
    public ResponseEntity<AlmacenResponse> actualizar(@PathVariable Long id, @Valid @RequestBody AlmacenRequest request) {
        return ResponseEntity.ok(almacenService.actualizar(id, request));
    }

    @Operation(summary = "Elimina un almacén")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        almacenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}