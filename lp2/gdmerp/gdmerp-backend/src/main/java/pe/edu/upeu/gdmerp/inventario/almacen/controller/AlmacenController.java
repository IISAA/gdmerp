package pe.edu.upeu.gdmerp.inventario.almacen.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenRequest;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.almacen.service.AlmacenService;
import java.util.List;

@Slf4j
@Tag(name = "Almacenes")
@RestController
@RequestMapping("/api/v1/almacenes")
@RequiredArgsConstructor
public class AlmacenController {
    private final AlmacenService almacenService;

    @Operation(summary = "Lista todos los almacenes")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public ResponseEntity<List<AlmacenResponse>> listar() {
        log.info("Listando Almacenes (Response DTO record limpio)...");
        return ResponseEntity.ok(almacenService.listar());
    }

    @Operation(summary = "Busca un almacén por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public ResponseEntity<AlmacenResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(almacenService.buscarPorId(id));
    }

    @Operation(summary = "Crea un nuevo almacén")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ResponseEntity<AlmacenResponse> crear(@Valid @RequestBody AlmacenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(almacenService.crear(request));
    }

    @Operation(summary = "Actualiza un almacén existente")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ResponseEntity<AlmacenResponse> actualizar(@PathVariable Long id, @Valid @RequestBody AlmacenRequest request) {
        return ResponseEntity.ok(almacenService.actualizar(id, request));
    }

    @Operation(summary = "Elimina un almacén")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        almacenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
