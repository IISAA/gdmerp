package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

public record OrdenProduccionResponse(Long id, String producto, Integer cantidadPlanificada, String estado) {
}