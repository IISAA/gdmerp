package pe.edu.upeu.gdmerp.inventario.producto.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.gdmerp.inventario.producto.dto.CreateProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.dto.UpdateProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import org.springframework.data.domain.Sort;
import java.util.List;

@Tag(name = "Productos")
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService productoService;

    @Operation(summary = "Lista todos los productos")
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(productoService.listar());
    }

    @Operation(summary = "Filtro dinámico de productos con ordenamiento")
    @GetMapping("/filtrar")
    public ResponseEntity<List<ProductoResponse>> filtrar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Integer minStock,
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
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody CreateProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @Operation(summary = "Actualiza un producto")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody UpdateProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @Operation(summary = "Elimina un producto")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}