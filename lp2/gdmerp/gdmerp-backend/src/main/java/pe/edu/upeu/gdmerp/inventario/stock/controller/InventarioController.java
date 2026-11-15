package pe.edu.upeu.gdmerp.inventario.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.inventario.stock.dto.AjusteInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.stock.dto.MovimientoInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.stock.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.inventario.stock.service.InventarioService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "Inventario - Stock y Movimientos")
@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final InventarioOperacionService inventarioOperacionService;

    @Operation(summary = "Lista todo el stock de inventario o filtra por tipo de producto")
    @GetMapping
    public ResponseEntity<List<StockInventarioResponse>> listar(
            @RequestParam(name = "tipoProducto", required = false) String tipoProducto) {
        return ResponseEntity.ok(inventarioService.listar(tipoProducto));
    }

    @Operation(summary = "Filtro dinámico de inventario con ordenamiento")
    @GetMapping("/filtrar")
    public ResponseEntity<List<StockInventarioResponse>> filtrar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) BigDecimal minStock,
            Sort sort) {
        return ResponseEntity.ok(inventarioService.filtrarDinamicamente(categoriaId, almacenId, minStock, sort));
    }

    @Operation(summary = "Lista las existencias de un almacén específico")
    @GetMapping("/por-almacen/{almacenId}")
    public ResponseEntity<List<StockInventarioResponse>> listarPorAlmacen(@PathVariable Long almacenId) {
        return ResponseEntity.ok(inventarioService.listarPorAlmacen(almacenId));
    }

    @Operation(summary = "Busca un registro de stock por su ID de inventario")
    @GetMapping("/{id}")
    public ResponseEntity<StockInventarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(inventarioService.buscarPorId(id));
    }

    @Operation(summary = "Consulta el stock de inventario por ID de producto del catálogo")
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<StockInventarioResponse> buscarPorProductoId(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.buscarPorProductoId(productoId));
    }

    @Operation(summary = "Consulta el Kardex de movimientos de un producto por su productoId")
    @GetMapping("/producto/{productoId}/movimientos")
    public ResponseEntity<List<MovimientoInventarioResponse>> listarMovimientosPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.listarMovimientosPorProducto(productoId));
    }

    @Operation(summary = "Registra un ítem de stock vinculado a un producto del catálogo")
    @PostMapping
    public ResponseEntity<StockInventarioResponse> registrar(@Valid @RequestBody StockInventarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.registrarStock(request));
    }

    @Operation(summary = "Actualiza un registro de stock existente")
    @PutMapping("/{id}")
    public ResponseEntity<StockInventarioResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody StockInventarioRequest request) {
        return ResponseEntity.ok(inventarioService.actualizarStock(id, request));
    }

    @Operation(summary = "Elimina un registro de stock sin existencias")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        inventarioService.eliminarStock(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Registra una merma o ajuste manual de inventario y emite impacto contable a Finanzas")
    @PostMapping("/producto/{productoId}/ajustes")
    public ResponseEntity<Map<String, Object>> registrarAjusteInventario(
            @PathVariable Long productoId,
            @Valid @RequestBody AjusteInventarioRequest request) {
        Long ajusteId = inventarioOperacionService.registrarAjusteInventario(
                productoId,
                request.diferencia(),
                request.motivoAjuste()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "ajusteId", ajusteId,
                "productoId", productoId,
                "diferencia", request.diferencia(),
                "motivoAjuste", request.motivoAjuste()
        ));
    }

    @Operation(summary = "Reporte semáforo de inventario y lotes")
    @GetMapping("/reporte-semaforo")
    public ResponseEntity<pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteSemaforoDTO> reporteSemaforo() {
        return ResponseEntity.ok(inventarioService.obtenerReporteSemaforo());
    }
}
