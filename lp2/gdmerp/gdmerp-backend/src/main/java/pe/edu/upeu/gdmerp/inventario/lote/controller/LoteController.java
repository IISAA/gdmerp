package pe.edu.upeu.gdmerp.inventario.lote.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteRequest;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteResponse;
import pe.edu.upeu.gdmerp.inventario.lote.service.LoteService;

import java.util.List;
import java.util.Map;

@Tag(name = "Lotes")
@RestController
@RequestMapping("/api/v1/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @Operation(summary = "Registra el ingreso de un nuevo lote y actualiza stock y Kardex")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ResponseEntity<LoteResponse> registrarIngreso(@Valid @RequestBody LoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteService.registrarIngreso(request));
    }

    @Operation(summary = "Lista todos los lotes ordenados por fecha de vencimiento")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public ResponseEntity<List<LoteResponse>> listarTodos() {
        return ResponseEntity.ok(loteService.listarTodos());
    }

    @Operation(summary = "Obtiene el siguiente código correlativo de lote (LT-YYYY-NNN)")
    @GetMapping("/siguiente-codigo")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ResponseEntity<Map<String, String>> obtenerSiguienteCodigo() {
        return ResponseEntity.ok(Map.of("codigo", loteService.obtenerSiguienteCodigo()));
    }

    @Operation(summary = "Lista los lotes de un producto específico ordenados por FEFO")
    @GetMapping("/producto/{productoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public ResponseEntity<List<LoteResponse>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(loteService.listarPorProducto(productoId));
    }
}