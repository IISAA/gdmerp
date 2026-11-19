package pe.edu.upeu.gdmerp.inventario.lote.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteRequest;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteResponse;
import pe.edu.upeu.gdmerp.inventario.lote.service.LoteService;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @PostMapping
    public ResponseEntity<LoteResponse> registrarIngreso(@Valid @RequestBody LoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteService.registrarIngreso(request));
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<LoteResponse>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(loteService.listarPorProducto(productoId));
    }
}