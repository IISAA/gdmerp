package pe.edu.upeu.gdmerp.finanzas.sesioncaja.controller;

import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.DenominacionMonedaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DetalleArqueoEfectivo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.AperturaSesionRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreCajaReporteDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreSesionRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.service.SesionCajaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finanzas/sesiones")
@Tag(name = "Turnos y Sesiones de Caja", description = "Gestión de aperturas, cierres, arqueos de gaveta física M:N y control de liquidez RN-FIN-01")
public class SesionCajaController {

    private final SesionCajaService sesionCajaService;

    public SesionCajaController(SesionCajaService sesionCajaService) {
        this.sesionCajaService = sesionCajaService;
    }

    @PostMapping("/abrir")
    @Operation(summary = "Aperturar nuevo turno diario con saldo inicial en efectivo")
    public ResponseEntity<SesionCajaResponse> abrirSesion(@Valid @RequestBody AperturaSesionRequest request) {
        SesionCajaResponse response = sesionCajaService.abrirSesion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/cerrar")
    @Operation(summary = "Cerrar turno con arqueo físico de billetes y monedas (DetalleArqueoEfectivo M:N) y cálculo de descuadre")
    public ResponseEntity<SesionCajaResponse> cerrarSesion(
            @PathVariable Long id,
            @Valid @RequestBody CierreSesionRequest request) {
        SesionCajaResponse response = sesionCajaService.cerrarSesion(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/movimientos")
    @Operation(summary = "Registrar ingreso o egreso en efectivo en gaveta física con validación de solvencia RN-FIN-01")
    public ResponseEntity<MovimientoCajaResponse> registrarMovimiento(
            @PathVariable Long id,
            @Valid @RequestBody MovimientoCajaRequest request) {
        MovimientoCajaResponse response = sesionCajaService.registrarMovimiento(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar historial completo de turnos de caja para monitoreo y auditoría")
    public ResponseEntity<List<SesionCajaResponse>> listarSesiones() {
        return ResponseEntity.ok(sesionCajaService.listarSesiones());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle completo de arqueo, movimientos y desglose de denominaciones por ID de sesión")
    public ResponseEntity<SesionCajaDetalleResponse> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(sesionCajaService.obtenerDetalle(id));
    }

    @GetMapping("/activa")
    @Operation(summary = "Obtener la sesión de caja actualmente abierta en el sistema")
    public ResponseEntity<SesionCajaResponse> obtenerUltimaSesionActiva() {
        return ResponseEntity.ok(sesionCajaService.obtenerUltimaSesionActiva());
    }

    @GetMapping("/activa/usuario/{usuarioId}")
    @Operation(summary = "Obtener la sesión de caja abierta asociada a un usuario específico")
    public ResponseEntity<SesionCajaResponse> obtenerSesionActivaUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(sesionCajaService.obtenerSesionActivaUsuario(usuarioId));
    }

    @GetMapping("/{id}/datos-cierre")
    @Operation(summary = "Generar datos estructurados para el ticket térmico de cierre de caja")
    public ResponseEntity<CierreCajaReporteDto> obtenerDatosCierre(@PathVariable Long id) {
        return ResponseEntity.ok(sesionCajaService.generarDatosReporte(id));
    }

    @GetMapping("/denominaciones")
    @Operation(summary = "Listar catálogo de billetes y monedas activas para el arqueo físico de caja")
    public ResponseEntity<List<pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.DenominacionMonedaResponse>> listarDenominaciones() {
        return ResponseEntity.ok(sesionCajaService.listarDenominaciones());
    }
}
