package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CrearCreditoRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.RegistrarAbonoRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.service.CuentaPorCobrarService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finanzas/creditos")
@RequiredArgsConstructor
@Tag(name = "Cuentas por Cobrar", description = "Gestión de créditos comerciales a clientes, fiados, control de saldos y amortizaciones con impacto en caja (Odoo 17 workflow)")
public class CuentaPorCobrarController {

    private final CuentaPorCobrarService cuentaPorCobrarService;

    @GetMapping
    @Operation(summary = "Listar créditos y cuentas por cobrar con filtro opcional por estado")
    public ResponseEntity<List<CuentaPorCobrarResponse>> listarCreditos(
            @RequestParam(required = false) EstadoCredito estado
    ) {
        return ResponseEntity.ok(cuentaPorCobrarService.listarTodos(estado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de crédito e historial completo de amortizaciones/abonos")
    public ResponseEntity<CuentaPorCobrarDetalleResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaPorCobrarService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo crédito o venta al fiado a cliente")
    public ResponseEntity<CuentaPorCobrarResponse> registrarCredito(@Valid @RequestBody CrearCreditoRequest request) {
        CuentaPorCobrarResponse response = cuentaPorCobrarService.registrarCredito(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/abonos")
    @Operation(summary = "Registrar cobro o amortización de crédito con sincronización automática al turno de caja activo")
    public ResponseEntity<CuentaPorCobrarDetalleResponse> registrarAbono(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarAbonoRequest request
    ) {
        CuentaPorCobrarDetalleResponse response = cuentaPorCobrarService.registrarAbono(id, request);
        return ResponseEntity.ok(response);
    }
}
