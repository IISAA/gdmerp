package pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto;

import java.math.BigDecimal;

public record CentroTrabajoResponse(
    Long id,
    String nombre,
    BigDecimal capacidad,
    String codigo,
    BigDecimal costoHora
) {}
