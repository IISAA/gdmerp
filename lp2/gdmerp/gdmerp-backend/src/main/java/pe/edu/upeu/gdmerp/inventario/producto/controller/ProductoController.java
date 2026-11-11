package pe.edu.upeu.gdmerp.inventario.producto.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.inventario.producto.dto.AjusteInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "Productos")
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService productoService;
    private final InventarioOperacionService inventarioOperacionService;

    @Operation(summary = "Lista todos los productos o filtra por tipo de producto")
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar(
            @RequestParam(name = "tipoProducto", required = false) String tipoProducto) {
        return ResponseEntity.ok(productoService.listar(tipoProducto));
    }

    @Operation(summary = "Sugerir SKU automático para previsualización en tiempo real")
    @GetMapping("/sugerir-sku")
    public ResponseEntity<Map<String, String>> sugerirSku(
            @RequestParam("almacenId") Long almacenId,
            @RequestParam("nombreProducto") String nombreProducto) {
        return ResponseEntity.ok(productoService.sugerirSku(almacenId, nombreProducto));
    }

    @Operation(summary = "Filtro dinámico de productos con ordenamiento")
    @GetMapping("/filtrar")
    public ResponseEntity<List<ProductoResponse>> filtrar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) BigDecimal minStock,
            Sort sort
    ) {
        return ResponseEntity.ok(productoService.filtrarDinamicamente(categoriaId, almacenId, minStock, sort));
    }

    @Operation(summary = "Navegación controlada: lista los productos de un almacén")
    @GetMapping("/por-almacen/{almacenId}")
    public ResponseEntity<List<ProductoResponse>> listarPorAlmacen(@PathVariable Long almacenId) {
        return ResponseEntity.ok(productoService.listarPorAlmacen(almacenId));
    }

    @Operation(summary = "Busca producto por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }

    @Operation(summary = "Crea un producto")
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @Operation(summary = "Actualiza un producto")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @Operation(summary = "Elimina un producto")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Registra una merma o ajuste manual de inventario y emite impacto contable a Finanzas")
    @PostMapping("/{id}/ajustes")
    public ResponseEntity<Map<String, Object>> registrarAjusteInventario(
            @PathVariable Long id,
            @Valid @RequestBody AjusteInventarioRequest request) {
        Long ajusteId = inventarioOperacionService.registrarAjusteInventario(
                id,
                request.diferencia(),
                request.motivoAjuste()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "ajusteId", ajusteId,
                "productoId", id,
                "diferencia", request.diferencia(),
                "motivoAjuste", request.motivoAjuste()
        ));
    }
}
