package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;

public record OrdenProduccionResponse(
    Long id, 
    String producto, 
    Integer cantidadPlanificada, 
    String estado,
    CentroTrabajoResponse centroTrabajo // Relación anidada
) {}
