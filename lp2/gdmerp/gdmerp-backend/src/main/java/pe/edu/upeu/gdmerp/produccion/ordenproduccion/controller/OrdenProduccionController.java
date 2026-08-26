package pe.edu.upeu.gdmerp.produccion.ordenproduccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.service.OrdenProduccionService;
import java.util.List;

@Tag(name = "Órdenes de Producción")
@RestController
@RequestMapping("/api/v1/ordenes-produccion")
@RequiredArgsConstructor
public class OrdenProduccionController {
    private final OrdenProduccionService ordenProduccionService;

    @Operation(summary = "Lista las órdenes de producción registradas")
    @GetMapping
    public ResponseEntity<List<OrdenProduccionResponse>> listar() {
        return ResponseEntity.ok(ordenProduccionService.listar());
    }
}