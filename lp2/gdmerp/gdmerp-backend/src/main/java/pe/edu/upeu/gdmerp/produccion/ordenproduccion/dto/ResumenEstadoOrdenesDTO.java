package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;

public record ResumenEstadoOrdenesDTO(
    EstadoOrdenProduccion estado,
    Long cantidadOrdenes
) {}