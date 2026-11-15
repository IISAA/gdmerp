package pe.edu.upeu.gdmerp.catalogo.producto.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.service.ProductoService;

import java.util.List;
import java.util.Map;

@Tag(name = "Catálogo - Productos", description = "Fuente única de verdad comercial para productos e insumos del ERP")
@RestController("catalogoProductoController")
@RequestMapping({"/api/v1/catalogo/productos", "/api/v1/productos"})
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(@Qualifier("catalogoProductoService") ProductoService productoService) {
        this.productoService = productoService;
    }

    @Operation(summary = "Listar productos con filtros opcionales por categoría o tipo de producto")
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) TipoProducto tipoProducto) {
        if (tipoProducto != null) {
            return ResponseEntity.ok(productoService.listarPorTipo(tipoProducto));
        }
        return ResponseEntity.ok(productoService.listar(categoriaId));
    }

    @Operation(summary = "Obtener producto por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtener(id));
    }

    @Operation(summary = "Sugerir un SKU único según contexto y nombre de producto")
    @GetMapping("/sugerir-sku")
    public ResponseEntity<Map<String, String>> sugerirSku(
            @RequestParam(required = false) String contexto,
            @RequestParam String nombreProducto) {
        return ResponseEntity.ok(productoService.sugerirSku(contexto, nombreProducto));
    }

    @Operation(summary = "Crear nuevo producto en el catálogo")
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @Operation(summary = "Actualizar producto existente")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar producto por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
