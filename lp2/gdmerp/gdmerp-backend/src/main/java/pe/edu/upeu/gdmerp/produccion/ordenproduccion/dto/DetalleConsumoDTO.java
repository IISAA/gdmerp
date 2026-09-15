package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import java.math.BigDecimal;

public record DetalleConsumoDTO(
    Long productoId,
    String nombreProducto,
    Integer cantidadRequerida,
    BigDecimal costoUnitario,
    BigDecimal costoTotal
) {}