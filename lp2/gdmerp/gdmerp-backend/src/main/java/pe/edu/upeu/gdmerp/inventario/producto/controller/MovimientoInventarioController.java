package pe.edu.upeu.gdmerp.inventario.producto.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.inventario.producto.dto.MovimientoInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.MovimientoInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.producto.service.MovimientoInventarioService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/movimientos")
@RequiredArgsConstructor
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoService;

    @GetMapping
    public ResponseEntity<List<MovimientoInventarioResponse>> listarKardex(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String tipoMovimiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(movimientoService.listarKardex(productoId, tipoMovimiento, desde, hasta));
    }

    @PostMapping
    public ResponseEntity<MovimientoInventarioResponse> registrarMovimiento(
            @Valid @RequestBody MovimientoInventarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movimientoService.registrarMovimientoManual(request));
    }
}