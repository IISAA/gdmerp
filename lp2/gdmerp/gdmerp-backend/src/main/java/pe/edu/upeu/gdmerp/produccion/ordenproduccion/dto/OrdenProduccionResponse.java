package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import java.math.BigDecimal;
import java.util.List;

public record OrdenProduccionResponse(
    Long id, 
    Long productoTerminadoId,
    String producto, 
    Integer cantidadPlanificada, 
    String estado,
    String loteGenerado,
    BigDecimal totalCostoInsumos,
    CentroTrabajoResponse centroTrabajo,
    List<DetalleOrdenResponse> detalles
) {}