package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenProduccionResponse(
    Long id, 
    Long productoTerminadoId,
    String producto, 
    Integer cantidadPlanificada, 
    EstadoOrdenProduccion estado,
    LocalDateTime fechaPlanificadaInicio,
    LocalDateTime fechaEstimadaFin,
    String loteGenerado,
    BigDecimal totalCostoInsumos,
    CentroTrabajoResponse centroTrabajo,
    List<DetalleOrdenResponse> detalles
) {}