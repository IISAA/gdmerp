package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import java.math.BigDecimal;

public record DetalleOrdenResponse(
    Long id,
    Long productoId,
    String nombreProducto,
    Integer cantidadRequerida,
    BigDecimal costoUnitario,
    BigDecimal subtotal
) {}