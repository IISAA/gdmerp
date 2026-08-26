package pe.edu.upeu.gdmerp.inventario.almacen.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.almacen.service.AlmacenService;
import java.util.List;

@Tag(name = "Almacenes")
@RestController
@RequestMapping("/api/v1/almacenes")
@RequiredArgsConstructor
public class AlmacenController {
    private final AlmacenService almacenService;

    @Operation(summary = "Lista los almacenes registrados")
    @GetMapping
    public ResponseEntity<List<AlmacenResponse>> listar() {
        return ResponseEntity.ok(almacenService.listar());
    }
}