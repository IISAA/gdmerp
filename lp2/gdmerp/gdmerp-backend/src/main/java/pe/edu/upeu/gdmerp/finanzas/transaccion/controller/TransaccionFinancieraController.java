package pe.edu.upeu.gdmerp.finanzas.transaccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.FlujoCajaResumenResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraRequest;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.service.FinanzasService;

import java.util.List;

@Tag(name = "Finanzas y Flujo de Caja (Agro Peruvian Foods)")
@RestController
@RequestMapping("/api/v1/finanzas")
public class TransaccionFinancieraController {
    private final FinanzasService finanzasService;

    public TransaccionFinancieraController(FinanzasService finanzasService) {
        this.finanzasService = finanzasService;
    }

    @Operation(summary = "Navegación controlada: Lista las transacciones financieras (recurso principal), opcionalmente filtradas por categoría de clasificación, tipo o estado")
    @GetMapping("/transacciones")
    public ResponseEntity<List<TransaccionFinancieraResponse>> listar(
            @RequestParam(required = false) TipoFinanciero tipo,
            @RequestParam(required = false) EstadoFinanciero estado,
            @RequestParam(required = false) Long categoriaFinancieraId) {
        return ResponseEntity.ok(finanzasService.listarTransacciones(tipo, estado, categoriaFinancieraId));
    }

    @Operation(summary = "Consulta una transacción financiera por ID")
    @GetMapping("/transacciones/{id}")
    public ResponseEntity<TransaccionFinancieraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(finanzasService.obtenerTransaccion(id));
    }

    @Operation(summary = "Registra un nuevo ingreso o egreso financiero con su categoría y comprobante")
    @PostMapping("/transacciones")
    @ResponseStatus(HttpStatus.CREATED)
    public TransaccionFinancieraResponse crear(@Valid @RequestBody TransaccionFinancieraRequest request) {
        return finanzasService.crearTransaccion(request);
    }

    @Operation(summary = "Actualiza una transacción financiera existente")
    @PutMapping("/transacciones/{id}")
    public ResponseEntity<TransaccionFinancieraResponse> actualizar(@PathVariable Long id, @Valid @RequestBody TransaccionFinancieraRequest request) {
        return ResponseEntity.ok(finanzasService.actualizarTransaccion(id, request));
    }

    @Operation(summary = "Cambia el estado de una transacción (PENDIENTE, PAGADO, ANULADO)")
    @PatchMapping("/transacciones/{id}/estado")
    public ResponseEntity<TransaccionFinancieraResponse> cambiarEstado(@PathVariable Long id, @RequestParam EstadoFinanciero estado) {
        return ResponseEntity.ok(finanzasService.cambiarEstadoTransaccion(id, estado));
    }

    @Operation(summary = "Elimina una transacción financiera")
    @DeleteMapping("/transacciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        finanzasService.eliminarTransaccion(id);
    }

    @Operation(summary = "Obtiene el reporte consolidado de Flujo de Caja (Ingresos, Egresos, Cuentas por Cobrar y por Pagar)")
    @GetMapping("/resumen-flujo-caja")
    public ResponseEntity<FlujoCajaResumenResponse> obtenerResumenFlujoCaja() {
        return ResponseEntity.ok(finanzasService.obtenerResumenFlujoCaja());
    }
}
