package pe.edu.upeu.gdmerp.inventario.unidadmedida.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.SincronizacionEstandaresResponse;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.UnidadMedidaRequest;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.UnidadMedidaResponse;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.service.UnidadMedidaService;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventario/unidades-medida", "/api/v1/unidades-medida"})
@RequiredArgsConstructor
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    /**
     * Lista todas las unidades de medida.
     * Si la tabla tiene menos de 10 registros, la capa de servicio dispara
     * automáticamente la sincronización del catálogo estándar antes de responder.
     *
     * @param activo {@code true} → sólo activas; {@code null} o ausente → todas.
     */
    @GetMapping
    public ResponseEntity<List<UnidadMedidaResponse>> listar(
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(unidadMedidaService.listar(activo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadMedidaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(unidadMedidaService.obtenerPorId(id));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<UnidadMedidaResponse> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(unidadMedidaService.obtenerPorCodigo(codigo));
    }

    @PostMapping
    public ResponseEntity<UnidadMedidaResponse> crear(@Valid @RequestBody UnidadMedidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unidadMedidaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnidadMedidaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UnidadMedidaRequest request) {
        return ResponseEntity.ok(unidadMedidaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UnidadMedidaResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        unidadMedidaService.cambiarEstado(id, activo);
        return ResponseEntity.ok(unidadMedidaService.obtenerPorId(id));
    }

    /**
     * Endpoint de sincronización forzada del catálogo estándar SI / UN-ECE / SUNAT.
     * <p>
     * Inserta unidad por unidad sólo las que aún no existen (idempotente).
     * Útil para entornos con BD parcialmente cargada o luego de un reset de datos.
     * </p>
     *
     * <pre>POST /api/v1/inventario/unidades-medida/cargar-estandares</pre>
     *
     * @return {@link SincronizacionEstandaresResponse} con conteo de insertadas/omitidas y lista de códigos nuevos.
     */
    @PostMapping("/cargar-estandares")
    public ResponseEntity<SincronizacionEstandaresResponse> cargarEstandares() {
        SincronizacionEstandaresResponse resultado = unidadMedidaService.sincronizarEstandares();
        return ResponseEntity.ok(resultado);
    }
}
