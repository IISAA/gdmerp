package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import java.util.List;

public record OrdenProduccionResponse(
    Long id, 
    Long productoTerminadoId,
    String producto, 
    Integer cantidadPlanificada, 
    String estado,
    String loteGenerado,
    CentroTrabajoResponse centroTrabajo,
    List<DetalleOrdenResponse> detalles
) {}