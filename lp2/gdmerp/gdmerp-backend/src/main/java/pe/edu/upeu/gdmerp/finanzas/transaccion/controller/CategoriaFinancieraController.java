package pe.edu.upeu.gdmerp.finanzas.transaccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraRequest;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.service.FinanzasService;

import java.util.List;

@Tag(name = "Categorías Financieras (Clasificación)")
@RestController
@RequestMapping("/api/v1/finanzas/categorias")
public class CategoriaFinancieraController {
    private final FinanzasService finanzasService;

    public CategoriaFinancieraController(FinanzasService finanzasService) {
        this.finanzasService = finanzasService;
    }

    @Operation(summary = "Lista las categorías financieras registradas")
    @GetMapping
    public ResponseEntity<List<CategoriaFinancieraResponse>> listar(@RequestParam(required = false) TipoFinanciero tipo) {
        return ResponseEntity.ok(finanzasService.listarCategorias(tipo));
    }

    @Operation(summary = "Consulta una categoría financiera por ID")
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaFinancieraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(finanzasService.obtenerCategoria(id));
    }

    @Operation(summary = "Navegación controlada: Lista las transacciones financieras pertenecientes a una categoría por su ID")
    @GetMapping("/{id}/transacciones")
    public ResponseEntity<List<TransaccionFinancieraResponse>> listarTransaccionesPorCategoria(@PathVariable Long id) {
        return ResponseEntity.ok(finanzasService.listarTransaccionesPorCategoria(id));
    }

    @Operation(summary = "Registra una nueva categoría financiera")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaFinancieraResponse crear(@Valid @RequestBody CategoriaFinancieraRequest request) {
        return finanzasService.crearCategoria(request);
    }

    @Operation(summary = "Actualiza una categoría financiera existente")
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaFinancieraResponse> actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaFinancieraRequest request) {
        return ResponseEntity.ok(finanzasService.actualizarCategoria(id, request));
    }

    @Operation(summary = "Elimina una categoría financiera")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        finanzasService.eliminarCategoria(id);
    }
}
