package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CrearCuentaPorPagarRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.RegistrarPagoCuentaPorPagarRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.EstadoCuentaPorPagar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.service.CuentaPorPagarService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finanzas/cuentas-por-pagar")
@RequiredArgsConstructor
public class CuentaPorPagarController {

    private final CuentaPorPagarService cuentaPorPagarService;

    @GetMapping
    public ResponseEntity<List<CuentaPorPagarResponse>> listar(
            @RequestParam(required = false) EstadoCuentaPorPagar estado) {
        return ResponseEntity.ok(cuentaPorPagarService.listarTodos(estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaPorPagarDetalleResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaPorPagarService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<CuentaPorPagarResponse> crear(@Valid @RequestBody CrearCuentaPorPagarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaPorPagarService.registrarCuentaPorPagar(request));
    }

    @PostMapping("/{id}/pagos")
    public ResponseEntity<CuentaPorPagarDetalleResponse> registrarPago(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarPagoCuentaPorPagarRequest request) {
        return ResponseEntity.ok(cuentaPorPagarService.registrarPago(id, request));
    }
}
