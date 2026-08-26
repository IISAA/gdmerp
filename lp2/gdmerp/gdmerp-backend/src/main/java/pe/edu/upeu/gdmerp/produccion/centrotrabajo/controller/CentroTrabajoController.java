package pe.edu.upeu.gdmerp.produccion.centrotrabajo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.service.CentroTrabajoService;
import java.util.List;

@Tag(name = "Centros de Trabajo")
@RestController
@RequestMapping("/api/v1/centros-trabajo")
@RequiredArgsConstructor
public class CentroTrabajoController {
    private final CentroTrabajoService centroTrabajoService;

    @Operation(summary = "Lista los centros de trabajo registrados")
    @GetMapping
    public ResponseEntity<List<CentroTrabajoResponse>> listar() {
        return ResponseEntity.ok(centroTrabajoService.listar());
    }
}